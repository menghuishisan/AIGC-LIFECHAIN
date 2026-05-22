"""
临时文件下载工具

使用 requests 下载（带重试 + 流式写入），避免 urllib 在 MinIO 抖动时直接崩溃，
并对大文件（视频/3D 模型）降低内存压力。
"""
from __future__ import annotations

import logging
import os
import tempfile
from contextlib import contextmanager
from typing import Iterator

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

logger = logging.getLogger(__name__)

# 单文件下载上限：与后端 WorkServiceImpl.MAX_FILE_SIZE_BYTES 对齐（500 MB）
MAX_DOWNLOAD_BYTES = 500 * 1024 * 1024
DOWNLOAD_CHUNK = 1024 * 1024  # 1 MB
DOWNLOAD_TIMEOUT = (10, 180)  # connect=10s, read=180s


def _build_session() -> requests.Session:
    session = requests.Session()
    retry = Retry(
        total=5,
        backoff_factor=0.5,
        status_forcelist=(500, 502, 503, 504),
        allowed_methods=frozenset(["GET"]),
    )
    adapter = HTTPAdapter(max_retries=retry)
    session.mount("http://", adapter)
    session.mount("https://", adapter)
    return session


@contextmanager
def download_to_tempfile(url: str, suffix: str = "") -> Iterator[str]:
    """
    下载远端文件到临时路径并在退出时删除。

    - 流式写入避免一次性加载到内存
    - 自动重试 5xx
    - 超过 MAX_DOWNLOAD_BYTES 直接报错
    """
    session = _build_session()
    fd, path = tempfile.mkstemp(suffix=suffix)
    os.close(fd)
    try:
        with session.get(url, stream=True, timeout=DOWNLOAD_TIMEOUT) as resp:
            resp.raise_for_status()
            written = 0
            with open(path, "wb") as f:
                for chunk in resp.iter_content(DOWNLOAD_CHUNK):
                    if not chunk:
                        continue
                    written += len(chunk)
                    if written > MAX_DOWNLOAD_BYTES:
                        raise RuntimeError(f"文件超过下载上限 {MAX_DOWNLOAD_BYTES} 字节")
                    f.write(chunk)
        logger.info("文件下载完成: %s (%d bytes)", path, written)
        yield path
    finally:
        if os.path.exists(path):
            try:
                os.unlink(path)
            except OSError as e:
                logger.warning("删除临时文件失败: %s, %s", path, e)
