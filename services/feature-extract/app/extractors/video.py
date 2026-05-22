"""
视频特征提取器（关键帧 PDQ 聚合）

通过 ffprobe 探测视频时长，按等距采样最多 16 个时间点，FFmpeg 精确 seek 抽帧，
逐帧计算 PDQ 256-bit 哈希后按位多数表决聚合为综合指纹。
比按 1 fps 抽前 N 帧更能代表整段视频。
"""
from __future__ import annotations

import json
import logging
import os
import subprocess
import tempfile

import numpy as np
import pdqhash
from PIL import Image

from app.extractors.base import HASH_BITS, Extractor, ExtractResult, bits_to_hex, extra_json
from app.extractors.download import download_to_tempfile

logger = logging.getLogger(__name__)

KEYFRAME_COUNT = 16
FRAME_SCALE = 320


def _probe_duration(video_path: str) -> float:
    """用 ffprobe 探测视频时长（秒）"""
    cmd = [
        "ffprobe", "-v", "error",
        "-show_entries", "format=duration",
        "-of", "json", video_path,
    ]
    proc = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
    if proc.returncode != 0:
        raise RuntimeError(f"ffprobe 失败: {proc.stderr[:200]}")
    info = json.loads(proc.stdout or "{}")
    duration = float(info.get("format", {}).get("duration", 0) or 0)
    if duration <= 0:
        raise RuntimeError("无法解析视频时长")
    return duration


def _extract_frame(video_path: str, timestamp: float, out_dir: str, idx: int) -> str | None:
    """精确 seek 到指定时间点抽 1 帧"""
    out_path = os.path.join(out_dir, f"frame_{idx:03d}.png")
    cmd = [
        "ffmpeg", "-y",
        "-ss", f"{timestamp:.3f}",
        "-i", video_path,
        "-vframes", "1",
        "-vf", f"scale={FRAME_SCALE}:{FRAME_SCALE}",
        "-loglevel", "error",
        out_path,
    ]
    proc = subprocess.run(cmd, capture_output=True, timeout=60)
    if proc.returncode != 0 or not os.path.exists(out_path):
        logger.warning("帧抽取失败 ts=%.3f: %s", timestamp, proc.stderr.decode(errors="ignore")[:200])
        return None
    return out_path


class PdqVideoExtractor(Extractor):
    """视频 PDQ 关键帧聚合提取器"""

    @property
    def algo(self) -> str:
        return "PDQ"

    @property
    def algo_version(self) -> str:
        return "1.0"

    def extract(self, file_path: str) -> ExtractResult:
        # 1. 探测时长 → 等距采样时间点
        duration = _probe_duration(file_path)
        timestamps = [duration * (i + 0.5) / KEYFRAME_COUNT for i in range(KEYFRAME_COUNT)]

        with tempfile.TemporaryDirectory() as out_dir:
            # 2. 精确 seek 抽帧（少量帧失败容忍）
            frame_paths = [
                p for p in (
                    _extract_frame(file_path, ts, out_dir, i)
                    for i, ts in enumerate(timestamps)
                ) if p is not None
            ]
            if not frame_paths:
                raise RuntimeError("FFmpeg 未能抽取到任何关键帧")

            # 3. 逐帧 PDQ
            bit_matrix = []
            for fp in frame_paths:
                img = Image.open(fp).convert("RGB")
                vec, _ = pdqhash.compute(np.array(img))
                bit_matrix.append(np.asarray(vec, dtype=np.uint8))

        # 4. 按位多数表决聚合（>50% 帧为 1 则该位为 1）
        stack = np.stack(bit_matrix)
        votes = stack.sum(axis=0)
        threshold = len(bit_matrix) / 2
        bits = (votes > threshold).astype(np.uint8)

        return ExtractResult(
            algo=self.algo,
            algo_version=self.algo_version,
            perceptual_hash=bits_to_hex(bits),
            extra={
                "frame_count": len(bit_matrix),
                "frames_planned": KEYFRAME_COUNT,
                "duration_sec": round(duration, 2),
            },
        )


def extract_video(file_url: str) -> dict:
    """主流程入口：下载文件 → 抽帧 → PDQ 聚合 → 返回响应字典"""
    logger.info("开始视频特征提取: %s", file_url[:100])
    extractor = PdqVideoExtractor()
    with download_to_tempfile(file_url, suffix=".video") as path:
        result = extractor.extract(path)
    logger.info("视频特征提取完成: hash=%s, frames=%s",
                result.perceptual_hash[:16], result.extra.get("frame_count"))
    return {
        "work_type": "VIDEO",
        "algo": result.algo,
        "algo_version": result.algo_version,
        "perceptual_hash": result.perceptual_hash,
        "extra": extra_json(result.extra),
    }
