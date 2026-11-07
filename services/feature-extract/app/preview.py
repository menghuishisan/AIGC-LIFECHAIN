"""生成独立的受限预览对象；市场访问永远不返回原始文件的下载地址。"""
from __future__ import annotations

import hashlib
import os
import subprocess
import tempfile
from pathlib import Path

from minio.error import S3Error
from PIL import Image, ImageDraw, ImageFont, ImageOps

from app.extractors.download import _storage_client, _validate_object_name, download_to_tempfile

PREVIEW_SECONDS = 30


def _image_preview(source: str, target: str) -> None:
    """缩小图片并把水印直接写入像素，不能通过关闭前端图层移除。"""
    with Image.open(source) as original:
        if original.width * original.height > 40_000_000:
            raise ValueError("图片像素数量超过预览处理上限")
        image = ImageOps.exif_transpose(original).convert("RGB")
        image.thumbnail((960, 960))
        overlay = Image.new("RGBA", image.size, (0, 0, 0, 0))
        draw = ImageDraw.Draw(overlay)
        font = ImageFont.load_default(size=max(12, min(image.size) // 24))
        for y in range(12, image.height, max(36, image.height // 4)):
            for x in range(8, image.width, max(100, image.width // 2)):
                draw.text((x, y), "LIFECHAIN PREVIEW", fill=(255, 255, 255, 190),
                          stroke_width=1, stroke_fill=(0, 0, 0, 150), font=font)
        Image.alpha_composite(image.convert("RGBA"), overlay).convert("RGB").save(target, "JPEG", quality=78)


def _media_preview(source: str, target: str, work_type: str) -> None:
    """重新编码独立片段，限制时长、网络协议、输入格式和执行时间。"""
    command = ["ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error", "-y",
               "-protocol_whitelist", "file,pipe", "-format_whitelist", "mov,matroska,avi,mp3,wav,ogg,flac,aac",
               "-i", source, "-t", str(PREVIEW_SECONDS), "-map_metadata", "-1", "-threads", "1"]
    if work_type == "AUDIO":
        command += ["-map", "0:a:0", "-vn", "-c:a", "aac", "-b:a", "96k"]
    else:
        command += ["-map", "0:v:0", "-map", "0:a?", "-vf", "scale=w='max(2,trunc(min(960,iw)/2)*2)':h=-2,setsar=1",
                    "-c:v", "libx264", "-pix_fmt", "yuv420p", "-preset", "veryfast", "-crf", "28", "-c:a", "aac", "-b:a", "96k"]
    command += ["-movflags", "+faststart", target]
    try:
        subprocess.run(command, check=True, timeout=180, stdin=subprocess.DEVNULL,
                       stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
    except (subprocess.SubprocessError, OSError) as error:
        raise RuntimeError("预览片段生成失败") from error


def generate_preview(object_name: str, work_type: str) -> dict:
    _validate_object_name(object_name)
    formats = {"IMAGE": ("jpg", "image/jpeg"), "AUDIO": ("m4a", "audio/mp4"), "VIDEO": ("mp4", "video/mp4")}
    if work_type not in formats:
        raise ValueError("此类型不提供文件预览")
    extension, content_type = formats[work_type]
    key = "previews/v1/" + hashlib.sha256(object_name.encode("utf-8")).hexdigest() + "." + extension
    client = _storage_client()
    bucket = os.environ["MINIO_BUCKET"]
    try:
        size = client.stat_object(bucket, key).size
    except S3Error as error:
        if error.code not in ("NoSuchKey", "NoSuchObject"):
            raise
        with download_to_tempfile(object_name) as source, tempfile.TemporaryDirectory() as temp_dir:
            target = str(Path(temp_dir) / ("preview." + extension))
            if work_type == "IMAGE":
                _image_preview(source, target)
            else:
                _media_preview(source, target, work_type)
            size = Path(target).stat().st_size
            if size == 0:
                raise RuntimeError("预览文件为空")
            client.fput_object(bucket, key, target, content_type=content_type)
    if size <= 0:
        raise RuntimeError("预览缓存文件无效")
    return {"object_name": key, "file_type": extension.upper(), "file_size": size,
            "duration_seconds": None if work_type == "IMAGE" else PREVIEW_SECONDS}
