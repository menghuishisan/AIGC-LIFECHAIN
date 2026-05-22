"""
特征提取算法插件注册中心

所有作品类型的提取器统一实现 Extractor 协议：输入文件路径，输出 256-bit 二进制指纹（hex 编码）。
新增类型只需 register("WORK_TYPE", YourExtractor()) 一行接通，无需改主流程。
"""
from __future__ import annotations

import json
import logging
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Any, Dict, Optional

import numpy as np

logger = logging.getLogger(__name__)

# 统一二进制指纹长度（与 Java 端 MilvusService.VECTOR_BITS 保持一致）
HASH_BITS = 256
HASH_BYTES = HASH_BITS // 8
HASH_HEX_LEN = HASH_BYTES * 2  # 64


@dataclass
class ExtractResult:
    """提取器输出"""
    algo: str
    algo_version: str
    perceptual_hash: str  # 256-bit hex 编码（64 字符）
    extra: Optional[Dict[str, Any]] = None

    def __post_init__(self):
        # 强校验：所有提取器必须输出 256-bit hex；上游统一调用、统一存储、统一比对
        if not isinstance(self.perceptual_hash, str) or len(self.perceptual_hash) != HASH_HEX_LEN:
            raise ValueError(
                f"perceptual_hash 必须为 {HASH_HEX_LEN} 字符 hex 字符串，实际长度 {len(self.perceptual_hash)}"
            )
        try:
            int(self.perceptual_hash, 16)
        except ValueError as e:
            raise ValueError(f"perceptual_hash 不是合法 hex: {e}") from e


class Extractor(ABC):
    """特征提取器基类"""

    @property
    @abstractmethod
    def algo(self) -> str:
        """算法名（PDQ/MINHASH/D2）"""

    @property
    @abstractmethod
    def algo_version(self) -> str:
        """算法版本"""

    @abstractmethod
    def extract(self, file_path: str) -> ExtractResult:
        """从本地文件路径提取 256-bit 指纹"""


# ========== 工具函数 ==========

def bits_to_hex(bits: np.ndarray) -> str:
    """将 0/1 位数组打包为 hex 字符串（256 bit → 64 字符）"""
    if bits.size != HASH_BITS:
        raise ValueError(f"位数组长度必须为 {HASH_BITS}，实际 {bits.size}")
    packed = np.packbits(bits.astype(np.uint8))
    return packed.tobytes().hex()


def int_to_bits(value: int, bits: int) -> np.ndarray:
    """将整数低 N 位转换为 0/1 数组（高位在前）"""
    return np.array([(value >> (bits - 1 - i)) & 1 for i in range(bits)], dtype=np.uint8)


def extra_json(extra: Optional[Dict[str, Any]]) -> Optional[str]:
    """将 extra 字典序列化为 JSON 字符串（用于响应）"""
    return json.dumps(extra, ensure_ascii=False) if extra else None


# ========== 注册中心 ==========

_REGISTRY: Dict[str, Extractor] = {}


def register(work_type: str, extractor: Extractor) -> None:
    """注册作品类型对应的提取器"""
    work_type = work_type.upper()
    if work_type in _REGISTRY:
        raise ValueError(f"作品类型已注册: {work_type}")
    _REGISTRY[work_type] = extractor
    logger.info("注册提取器: %s → %s/%s", work_type, extractor.algo, extractor.algo_version)


def get(work_type: str) -> Extractor:
    """按作品类型获取提取器（找不到则抛错，不允许静默跳过）"""
    work_type = work_type.upper()
    extractor = _REGISTRY.get(work_type)
    if extractor is None:
        raise ValueError(f"不支持的作品类型: {work_type}（已注册: {list(_REGISTRY.keys())}）")
    return extractor


def supported_types() -> list[str]:
    """已注册的全部作品类型"""
    return sorted(_REGISTRY.keys())
