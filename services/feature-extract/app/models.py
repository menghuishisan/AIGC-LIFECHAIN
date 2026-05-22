"""
特征提取服务数据模型

定义 HTTP 接口的请求和响应结构。所有作品类型统一输出 256-bit 二进制感知指纹（hex 编码 64 字符），
不同算法（PDQ/MINHASH/D2）只是生成路径不同，输出格式完全一致。
"""
from typing import Optional

from pydantic import BaseModel, Field


class ExtractRequest(BaseModel):
    """特征提取请求"""
    file_url: str = Field(description="文件的签名访问URL")
    work_type: str = Field(description="作品类型（IMAGE/VIDEO/AUDIO/TEXT/MODEL）")


class ExtractResponse(BaseModel):
    """特征提取响应"""
    work_type: str = Field(description="作品类型（透传请求值）")
    algo: str = Field(description="算法名称（PDQ/MINHASH/D2）")
    algo_version: str = Field(description="算法版本")
    perceptual_hash: str = Field(description="256-bit 二进制感知指纹的 hex 编码（64 字符）")
    extra: Optional[str] = Field(default=None, description="算法专属辅助信息（JSON 字符串）")
