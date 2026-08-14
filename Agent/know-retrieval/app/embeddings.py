"""bge-small-zh-v1.5 嵌入模型封装（惰性加载，本地优先）"""
import logging
import os
import threading

from sentence_transformers import SentenceTransformer

from .config import EMBEDDING_MODEL, MODEL_CACHE_DIR, LOCAL_MODEL_PATH

logger = logging.getLogger(__name__)

_model = None
_lock = threading.Lock()


def get_model() -> SentenceTransformer:
    """获取模型实例（惰性加载，线程安全）"""
    global _model
    if _model is None:
        with _lock:
            if _model is None:
                # 优先使用本地 snapshot 路径直接加载
                if os.path.isdir(LOCAL_MODEL_PATH):
                    logger.info("检测到本地模型路径，直接从路径加载: %s", LOCAL_MODEL_PATH)
                    try:
                        _model = SentenceTransformer(LOCAL_MODEL_PATH)
                        logger.info("模型加载完成（本地路径），维度: %s",
                                    _model.get_sentence_embedding_dimension())
                        return _model
                    except Exception as e:
                        logger.warning("本地路径加载失败: %s，回退到缓存模式", e)

                # 回退：使用模型名 + cache_folder + local_files_only
                logger.info("正在从缓存加载嵌入模型: %s ...", EMBEDDING_MODEL)
                os.makedirs(MODEL_CACHE_DIR, exist_ok=True)
                try:
                    _model = SentenceTransformer(
                        EMBEDDING_MODEL,
                        cache_folder=MODEL_CACHE_DIR,
                        local_files_only=True,
                    )
                    logger.info("模型加载完成（缓存），维度: %s",
                                _model.get_sentence_embedding_dimension())
                except Exception as e:
                    logger.warning("本地加载失败，尝试在线下载: %s", e)
                    _model = SentenceTransformer(
                        EMBEDDING_MODEL,
                        cache_folder=MODEL_CACHE_DIR,
                    )
                    logger.info("模型加载完成（在线），维度: %s",
                                _model.get_sentence_embedding_dimension())
    return _model


def embed_text(text: str) -> list[float]:
    """单条文本向量化"""
    model = get_model()
    vec = model.encode(text, normalize_embeddings=True)
    return vec.tolist()


def embed_batch(texts: list[str]) -> list[list[float]]:
    """批量文本向量化"""
    model = get_model()
    vecs = model.encode(texts, normalize_embeddings=True, batch_size=32)
    return vecs.tolist()
