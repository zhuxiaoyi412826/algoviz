"""配置管理"""
import os

# Chroma 持久化路径
CHROMA_PATH = os.getenv("CHROMA_PATH", "data/chroma_db")

# Collection 名称
COLLECTION_NAME = "interview_problems"

# 嵌入模型 - 使用本地已下载的模型，不再在线下载
# HuggingFace 缓存根目录
HF_CACHE_DIR = r"C:\Users\Administrator\.cache\huggingface\hub"
# 本地模型路径（直接指向已下载的 snapshot）
LOCAL_MODEL_PATH = os.getenv("LOCAL_MODEL_PATH",
    os.path.join(HF_CACHE_DIR, "models--BAAI--bge-small-zh-v1.5", "snapshots",
                 "7999e1d3359715c523056ef9478215996d62a620"))

# 嵌入模型 - 本地加载模式（local_files_only=True）
# 使用模型名 + cache_folder + local_files_only=True 从本地缓存加载
EMBEDDING_MODEL = os.getenv("MODEL_NAME", "BAAI/bge-small-zh-v1.5")

# bge-small-zh-v1.5 输出维度 512
VECTOR_DIMENSION = 512

# 默认相似度阈值
DEFAULT_THRESHOLD = 0.35

# 默认返回数量
DEFAULT_TOP_K = 10

# 服务端口
PORT = int(os.getenv("PORT", "8001"))

# 模型缓存目录
MODEL_CACHE_DIR = os.getenv("MODEL_CACHE_DIR", HF_CACHE_DIR)
