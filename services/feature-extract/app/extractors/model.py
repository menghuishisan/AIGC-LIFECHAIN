"""
3D 模型特征提取器（D2 形状描述符 → 256-bit 指纹）

D2 描述符（Osada 等, 2002）是经典的 3D 形状指纹算法：
1. 在网格表面均匀采样 N 个点
2. 随机配对采样 M 对，计算欧氏距离
3. 将距离归一化（除以包围盒对角线）后做直方图
4. 将每个 bin 的频率与全局中位数比较，得到 256-bit 二进制指纹

D2 对网格细分、平移、旋转、缩放都鲁棒（缩放靠归一化处理），且不依赖纹理/材质，
适合检测 3D 模型的"形状抄袭"。

trimesh 支持 OBJ/PLY/STL/GLB/GLTF 等常见格式；从原始 URL 嗅探扩展名以便正确选择 loader。
"""
from __future__ import annotations

import logging
import re
from urllib.parse import urlparse

import numpy as np
import trimesh

from app.extractors.base import HASH_BITS, Extractor, ExtractResult, bits_to_hex, extra_json
from app.extractors.download import download_to_tempfile

logger = logging.getLogger(__name__)

SAMPLE_POINTS = 4096      # 表面采样点数
PAIR_SAMPLES = 8192       # 距离对采样数（统计意义上足以稳定 256-bin 直方图）
HIST_BINS = HASH_BITS     # 256

# trimesh 支持的 3D 模型扩展名（受预装依赖限制；若用户上传其他格式，加 trimesh extras 即可扩展）
_SUPPORTED_EXTS = {"obj", "ply", "stl", "glb", "gltf", "off", "3mf", "dae"}


def _detect_file_type(url: str) -> str:
    """从 URL 提取扩展名，作为 trimesh.load 的 file_type 提示"""
    path = urlparse(url).path
    m = re.search(r"\.([a-zA-Z0-9]+)$", path)
    if m:
        ext = m.group(1).lower()
        if ext in _SUPPORTED_EXTS:
            return ext
    raise RuntimeError(f"无法从 URL 推断 3D 模型格式（支持：{','.join(sorted(_SUPPORTED_EXTS))}）")


def _load_mesh(file_path: str, file_type: str) -> trimesh.Trimesh:
    """加载 3D 模型，复合场景合并为单一 mesh"""
    obj = trimesh.load(file_path, file_type=file_type, force="mesh")
    if isinstance(obj, trimesh.Scene):
        obj = trimesh.util.concatenate(tuple(obj.geometry.values()))
    if not isinstance(obj, trimesh.Trimesh) or len(obj.vertices) == 0:
        raise RuntimeError("无法解析为有效 3D mesh")
    return obj


class D2ModelExtractor(Extractor):
    """3D 模型 D2 形状描述符提取器"""

    @property
    def algo(self) -> str:
        return "D2"

    @property
    def algo_version(self) -> str:
        return "1.0"

    def extract(self, file_path: str, file_type: str = "obj") -> ExtractResult:
        # 固定随机种子，保证同模型指纹稳定
        rng = np.random.default_rng(42)

        # 1. 加载并居中归一化（消除平移/缩放差异）
        mesh = _load_mesh(file_path, file_type)
        bbox = mesh.bounding_box.extents
        diag = float(np.linalg.norm(bbox))
        if diag <= 0:
            raise RuntimeError("模型包围盒退化（diag=0）")
        center = mesh.centroid
        verts = mesh.vertices - center

        # 2. 表面采样
        # trimesh.sample.sample_surface 按面积加权均匀采样
        try:
            samples, _ = trimesh.sample.sample_surface(mesh, SAMPLE_POINTS, seed=42)
        except TypeError:
            # 老版本 trimesh 不接受 seed 参数
            samples, _ = trimesh.sample.sample_surface(mesh, SAMPLE_POINTS)
        samples = np.asarray(samples) - center

        # 3. 随机配对计算距离
        idx_a = rng.integers(0, len(samples), PAIR_SAMPLES)
        idx_b = rng.integers(0, len(samples), PAIR_SAMPLES)
        dists = np.linalg.norm(samples[idx_a] - samples[idx_b], axis=1) / diag

        # 4. 距离直方图（256 bins，频率向量）
        hist, _ = np.histogram(dists, bins=HIST_BINS, range=(0.0, 1.0), density=False)
        freq = hist / max(hist.sum(), 1)

        # 5. 二值化：每个 bin 与全局中位数比较，生成 256-bit 指纹
        median = float(np.median(freq))
        bits = (freq > median).astype(np.uint8)

        return ExtractResult(
            algo=self.algo,
            algo_version=self.algo_version,
            perceptual_hash=bits_to_hex(bits),
            extra={
                "vertex_count": int(len(mesh.vertices)),
                "face_count": int(len(mesh.faces)),
                "sample_points": SAMPLE_POINTS,
                "pair_samples": PAIR_SAMPLES,
                "diag": round(diag, 4),
                "format": file_type,
            },
        )


def extract_model(file_url: str) -> dict:
    """主流程入口：从 URL 嗅探扩展名 → 下载 → 加载 mesh → D2 直方图 → 二值化 → 返回响应字典"""
    logger.info("开始 3D 模型特征提取: %s", file_url[:100])
    file_type = _detect_file_type(file_url)
    extractor = D2ModelExtractor()
    with download_to_tempfile(file_url, suffix=f".{file_type}") as path:
        result = extractor.extract(path, file_type=file_type)
    logger.info("3D 模型特征提取完成: hash=%s, vertices=%s, format=%s",
                result.perceptual_hash[:16], result.extra.get("vertex_count"), file_type)
    return {
        "work_type": "MODEL",
        "algo": result.algo,
        "algo_version": result.algo_version,
        "perceptual_hash": result.perceptual_hash,
        "extra": extra_json(result.extra),
    }
