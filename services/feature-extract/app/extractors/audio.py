"""
音频特征提取器（Chromaprint subfingerprints → MinHash）

Chromaprint 输出一串 32-bit 子指纹（每秒约 8 个），表征频域分布。
直接将整数序列丢进 IVF_FLAT/L2 在量级和度量上都不可比；
正确做法是把每个 32-bit 子指纹拆为 4 个 8-bit shingle，构造词集合后用 MinHash 压成 256-bit。
最终输出统一为 256-bit 二进制指纹。
"""
from __future__ import annotations

import json
import logging
import subprocess

from datasketch import MinHash

from app.extractors.base import HASH_BITS, Extractor, ExtractResult, bits_to_hex, extra_json
from app.extractors.download import download_to_tempfile

logger = logging.getLogger(__name__)


def _run_fpcalc(audio_path: str) -> tuple[list[int], float]:
    """
    调用 fpcalc 获取 raw 整数指纹和音频时长

    fpcalc 1.5.x 对许多正常音频会输出 stderr "Error decoding audio frame (End of file)"
    并以 returncode=3 退出，但 stdout 中仍包含完整指纹 JSON。
    因此判断成功的依据是"能否从 stdout 解析出非空 fingerprint"，而非 returncode。
    """
    cmd = ["fpcalc", "-raw", "-json", audio_path]
    proc = subprocess.run(cmd, capture_output=True, text=True, timeout=180)
    try:
        data = json.loads(proc.stdout or "{}")
    except json.JSONDecodeError:
        data = {}
    fingerprint = data.get("fingerprint") or []
    duration = float(data.get("duration") or 0)
    if not fingerprint:
        # 真正失败：stdout 不可解析或没产出指纹
        raise RuntimeError(
            f"fpcalc 未产出指纹（returncode={proc.returncode}）: {proc.stderr.strip()[:200]}"
        )
    return fingerprint, duration


def _shingles_from_subfingerprints(subfingerprints: list[int]) -> list[bytes]:
    """
    把 Chromaprint 的 32-bit 子指纹序列展开为 shingle 集合
    每个 32-bit 子指纹拆成 4 个 (位置, 字节值) 元组（位置标识区分高/低字节）
    """
    shingles: list[bytes] = []
    for fp in subfingerprints:
        # 取 32 位的低 32 位，避免负数
        v = fp & 0xFFFFFFFF
        for shift_idx in range(4):
            byte = (v >> (shift_idx * 8)) & 0xFF
            shingles.append(bytes([shift_idx, byte]))
    return shingles


class MinhashAudioExtractor(Extractor):
    """音频 MinHash 提取器"""

    @property
    def algo(self) -> str:
        return "MINHASH"

    @property
    def algo_version(self) -> str:
        return "1.0"

    def extract(self, file_path: str) -> ExtractResult:
        # 1. fpcalc 抽 Chromaprint 子指纹
        fingerprint, duration = _run_fpcalc(file_path)

        # 2. 子指纹 → shingle 集合
        shingles = _shingles_from_subfingerprints(fingerprint)

        # 3. MinHash 压缩为 256 个 32-bit 哈希值
        mh = MinHash(num_perm=HASH_BITS, seed=42)
        for s in shingles:
            mh.update(s)

        # 4. 取每个排列结果的最低位拼成 256-bit 指纹（datasketch 内部已是均匀分布）
        bits = (mh.hashvalues & 1).astype("uint8")

        return ExtractResult(
            algo=self.algo,
            algo_version=self.algo_version,
            perceptual_hash=bits_to_hex(bits),
            extra={
                "duration_sec": round(duration, 2),
                "subfingerprint_count": len(fingerprint),
                "shingle_count": len(shingles),
            },
        )


def extract_audio(file_url: str) -> dict:
    """主流程入口：下载 → fpcalc → MinHash → 返回响应字典"""
    logger.info("开始音频特征提取: %s", file_url[:100])
    extractor = MinhashAudioExtractor()
    with download_to_tempfile(file_url, suffix=".audio") as path:
        result = extractor.extract(path)
    logger.info("音频特征提取完成: hash=%s, duration=%.2fs",
                result.perceptual_hash[:16], result.extra.get("duration_sec", 0))
    return {
        "work_type": "AUDIO",
        "algo": result.algo,
        "algo_version": result.algo_version,
        "perceptual_hash": result.perceptual_hash,
        "extra": extra_json(result.extra),
    }
