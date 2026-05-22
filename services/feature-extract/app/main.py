"""
LifeChain 特征提取服务

FastAPI 应用入口，按作品类型路由到对应提取器：
- IMAGE → PDQ 256-bit
- VIDEO → 关键帧 PDQ 多数表决 256-bit
- AUDIO → Chromaprint subfingerprints + MinHash 256-bit
- TEXT  → PDF/DOCX/MD/TXT 抽文本 + jieba 5-gram + MinHash 256-bit
- MODEL → trimesh 表面采样 + D2 形状描述符 256-bit

所有作品类型统一输出 256-bit 二进制感知指纹（hex 编码 64 字符），
Java 端写入 5 个独立的 Milvus BinaryVector collection，HAMMING 距离比对。
"""
import logging

from fastapi import FastAPI, HTTPException

from app.extractors import base, image, video, audio, text, model
from app.models import ExtractRequest, ExtractResponse

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)

# 注册所有作品类型的提取器（启动时一次性完成，运行期不再修改）
base.register("IMAGE", image.PdqImageExtractor())
base.register("VIDEO", video.PdqVideoExtractor())
base.register("AUDIO", audio.MinhashAudioExtractor())
base.register("TEXT", text.MinhashTextExtractor())
base.register("MODEL", model.D2ModelExtractor())

# 主流程入口表（每种类型一个 download + extract 编排函数）
_HANDLERS = {
    "IMAGE": image.extract_image,
    "VIDEO": video.extract_video,
    "AUDIO": audio.extract_audio,
    "TEXT": text.extract_text,
    "MODEL": model.extract_model,
}

app = FastAPI(title="LifeChain Feature Extract Service", version="2.0.0")


@app.get("/health")
def health():
    """健康检查端点"""
    return {"status": "ok", "supported_types": base.supported_types()}


@app.post("/extract", response_model=ExtractResponse)
def extract(request: ExtractRequest):
    """
    执行特征提取

    根据 work_type 分发到对应算法，统一返回 256-bit hex 编码的二进制感知指纹。
    任何不支持的类型直接 400，不再静默跳过。
    """
    work_type = request.work_type.upper()
    logger.info("收到特征提取请求: work_type=%s", work_type)

    handler = _HANDLERS.get(work_type)
    if handler is None:
        supported = ",".join(base.supported_types())
        raise HTTPException(
            status_code=400,
            detail=f"不支持的作品类型: {work_type}（支持：{supported}）",
        )

    try:
        return handler(request.file_url)
    except HTTPException:
        raise
    except Exception as e:
        logger.error("特征提取失败: work_type=%s, error=%s", work_type, str(e), exc_info=True)
        raise HTTPException(status_code=500, detail=f"特征提取失败: {str(e)}")
