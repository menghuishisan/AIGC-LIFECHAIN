"""
图片特征提取器（PDQ Hash）

PDQ 是 Meta 开源的 256-bit 感知哈希，对缩放、轻微裁剪、JPEG 压缩有鲁棒性，
是图片查重的工业标准。直接输出 256-bit 二进制指纹（hex 编码）。
"""
from __future__ import annotations

import logging

import numpy as np
import pdqhash
from PIL import Image

from app.extractors.base import Extractor, ExtractResult, bits_to_hex, extra_json
from app.extractors.download import download_to_tempfile

logger = logging.getLogger(__name__)


class PdqImageExtractor(Extractor):
    """图片 PDQ 提取器"""

    @property
    def algo(self) -> str:
        return "PDQ"

    @property
    def algo_version(self) -> str:
        return "1.0"

    def extract(self, file_path: str) -> ExtractResult:
        """计算 PDQ 256-bit 感知哈希"""
        # 转为 RGB numpy 数组供 PDQ 算法使用
        img = Image.open(file_path).convert("RGB")
        img_array = np.array(img)

        # PDQ 计算：返回 256 位 0/1 向量 + 质量分（0-100）
        hash_vector, quality = pdqhash.compute(img_array)
        bits = np.asarray(hash_vector, dtype=np.uint8)

        return ExtractResult(
            algo=self.algo,
            algo_version=self.algo_version,
            perceptual_hash=bits_to_hex(bits),
            extra={"quality": int(quality), "width": img.width, "height": img.height},
        )


def extract_image(file_url: str) -> dict:
    """主流程入口：下载文件 → PDQ 提取 → 返回响应字典"""
    logger.info("开始图片特征提取: %s", file_url[:100])
    extractor = PdqImageExtractor()
    with download_to_tempfile(file_url, suffix=".img") as path:
        result = extractor.extract(path)
    logger.info("图片特征提取完成: hash=%s", result.perceptual_hash[:16])
    return {
        "work_type": "IMAGE",
        "algo": result.algo,
        "algo_version": result.algo_version,
        "perceptual_hash": result.perceptual_hash,
        "extra": extra_json(result.extra),
    }
