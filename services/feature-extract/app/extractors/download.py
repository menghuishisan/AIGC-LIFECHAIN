"""仅从受管 MinIO 私有桶流式读取文件，不接受远程 URL 或重定向。"""
from __future__ import annotations

import logging
import os
import tempfile
from contextlib import contextmanager
from typing import Iterator
from urllib.parse import urlsplit

from minio import Minio

logger = logging.getLogger(__name__)

# 单文件下载上限：与后端 WorkServiceImpl.MAX_FILE_SIZE_BYTES 对齐（500 MB）
MAX_DOWNLOAD_BYTES = 500 * 1024 * 1024
DOWNLOAD_CHUNK = 1024 * 1024  # 1 MB


def _validate_object_name(object_name: str) -> None:
    """对象键来自上传记录，禁止 URL、绝对路径和目录穿越。"""
    if (not object_name or object_name.startswith("/") or ":" in object_name
            or "\\" in object_name or any(ord(char) < 32 for char in object_name)
            or any(part in (".", "..", "") for part in object_name.split("/"))):
        raise ValueError("文件对象键无效")


def _storage_client() -> Minio:
    endpoint = urlsplit(os.environ["MINIO_ENDPOINT"])
    if (endpoint.scheme not in ("http", "https") or not endpoint.netloc
            or endpoint.path not in ("", "/") or endpoint.username or endpoint.password
            or endpoint.query or endpoint.fragment):
        raise ValueError("MINIO_ENDPOINT 必须是受管对象存储服务地址")
    return Minio(endpoint.netloc, access_key=os.environ["MINIO_ACCESS_KEY"],
                 secret_key=os.environ["MINIO_SECRET_KEY"], secure=endpoint.scheme == "https")


@contextmanager
def download_to_tempfile(object_name: str, suffix: str = "") -> Iterator[str]:
    """
    从配置的私有桶读取到临时文件，限制大小并始终释放响应和临时文件。

    - 流式写入避免一次性加载到内存
    - 超过 MAX_DOWNLOAD_BYTES 直接报错
    """
    _validate_object_name(object_name)
    response = _storage_client().get_object(os.environ["MINIO_BUCKET"], object_name)
    path = None
    try:
        if int(response.headers.get("Content-Length", "0")) > MAX_DOWNLOAD_BYTES:
            raise ValueError("文件超过下载上限")
        fd, path = tempfile.mkstemp(suffix=suffix)
        written = 0
        with os.fdopen(fd, "wb") as target:
            for chunk in response.stream(DOWNLOAD_CHUNK):
                written += len(chunk)
                if written > MAX_DOWNLOAD_BYTES:
                    raise ValueError("文件超过下载上限")
                target.write(chunk)
        if written == 0:
            raise ValueError("文件为空，无法提取特征")
        logger.info("文件下载完成: %s (%d bytes)", path, written)
        yield path
    finally:
        response.close()
        response.release_conn()
        if path is not None and os.path.exists(path):
            os.unlink(path)
