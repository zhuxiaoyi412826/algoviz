"""Chroma 向量数据库操作封装"""
import logging
import os

import chromadb
from chromadb.config import Settings

from .config import CHROMA_PATH, COLLECTION_NAME

logger = logging.getLogger(__name__)

_client = None
_collection = None


def get_client() -> chromadb.api.ClientAPI:
    """获取 Chroma 客户端（单例）"""
    global _client
    if _client is None:
        os.makedirs(CHROMA_PATH, exist_ok=True)
        _client = chromadb.PersistentClient(
            path=CHROMA_PATH,
            settings=Settings(anonymized_telemetry=False, allow_reset=True),
        )
        logger.info("Chroma 客户端初始化完成，路径: %s", CHROMA_PATH)
    return _client


def get_collection():
    """获取或创建 Collection（cosine 距离）"""
    global _collection
    if _collection is None:
        client = get_client()
        _collection = client.get_or_create_collection(
            name=COLLECTION_NAME,
            metadata={"hnsw:space": "cosine", "description": "面试题目向量"},
        )
        logger.info("Collection '%s' 就绪，当前向量数: %d", COLLECTION_NAME, _collection.count())
    return _collection


def upsert_problem(problem_id: int, text: str, metadata: dict):
    """单条题目向量入库（upsert 覆盖）"""
    from .embeddings import embed_text

    vec = embed_text(text)
    col = get_collection()
    col.upsert(
        ids=[str(problem_id)],
        embeddings=[vec],
        documents=[text],
        metadatas=[metadata],
    )
    return True


def upsert_batch(items: list[dict]):
    """批量入库
    items: [{"id": int, "text": str, "metadata": dict}, ...]
    """
    from .embeddings import embed_batch

    if not items:
        return {"success": 0, "failed": 0}

    texts = [item["text"] for item in items]
    vecs = embed_batch(texts)
    ids = [str(item["id"]) for item in items]
    metadatas = [item["metadata"] for item in items]

    col = get_collection()
    col.upsert(ids=ids, embeddings=vecs, documents=texts, metadatas=metadatas)
    return {"success": len(items), "failed": 0}


def delete_problem(problem_id: int):
    """删除单条向量"""
    col = get_collection()
    col.delete(ids=[str(problem_id)])
    return True


def search(query_vector: list[float], top_k: int = 10):
    """语义检索
    返回: [{"id": str, "distance": float, "document": str, "metadata": dict}, ...]
    """
    col = get_collection()
    try:
        results = col.query(
            query_embeddings=[query_vector],
            n_results=top_k,
            include=["metadatas", "documents", "distances"],
        )
    except TypeError:
        # 某些版本不支持 include 参数
        results = col.query(
            query_embeddings=[query_vector],
            n_results=top_k,
        )
    return format_results(results)


def search_by_text(query_text: str, top_k: int = 10):
    """通过文本进行语义检索（内部自动向量化）"""
    from .embeddings import embed_text

    vec = embed_text(query_text)
    return search(vec, top_k)


def format_results(raw: dict) -> list[dict]:
    """将 Chroma 原始返回格式化为统一列表"""
    if not raw or not raw.get("ids") or not raw["ids"][0]:
        return []

    items = []
    ids = raw["ids"][0]
    distances = raw.get("distances", [[]])[0]
    documents = raw.get("documents", [[]])[0]
    metadatas = raw.get("metadatas", [[]])[0]

    for i, rid in enumerate(ids):
        dist = distances[i] if i < len(distances) else 1.0
        # cosine 距离 [0, 2] → 相似度 [0, 1]
        similarity = max(0.0, 1.0 - dist / 2.0)
        items.append({
            "id": rid,
            "distance": dist,
            "similarity": similarity,
            "document": documents[i] if i < len(documents) else "",
            "metadata": metadatas[i] if i < len(metadatas) else {},
        })
    return items


def count() -> int:
    """返回当前向量总数"""
    return get_collection().count()


def get_dimension() -> int:
    """返回向量维度（通过 get 一条数据获取）"""
    col = get_collection()
    n = col.count()
    if n == 0:
        return 0
    # 取第一条向量看长度
    try:
        sample = col.get(limit=1, include=["embeddings"])
        embeddings = sample.get("embeddings", None)
        if embeddings is not None and len(embeddings) > 0:
            emb = embeddings[0]
            # 兼容 numpy 数组和普通列表
            if hasattr(emb, 'shape'):
                return int(emb.shape[0])
            return len(emb)
    except TypeError:
        # 某些版本不支持 include 参数
        sample = col.get(limit=1)
        if hasattr(sample, 'get'):
            embeddings = sample.get("embeddings", None)
            if embeddings is not None and len(embeddings) > 0:
                emb = embeddings[0]
                if hasattr(emb, 'shape'):
                    return int(emb.shape[0])
                return len(emb)
    return 0


def get_vectors_page(page: int = 1, page_size: int = 50) -> dict:
    """分页获取向量列表（含 metadata、document 和向量数值）"""
    col = get_collection()
    total = col.count()
    if total == 0:
        return {"total": 0, "page": page, "pageSize": page_size, "vectors": []}

    page = max(1, page)
    offset = (page - 1) * page_size

    # 获取包含 embeddings 的完整数据
    try:
        result = col.get(
            limit=page_size,
            offset=offset,
            include=["metadatas", "documents", "embeddings"],
        )
    except TypeError:
        # 某些版本不支持 include 参数
        result = col.get(
            limit=page_size,
            offset=offset,
        )

    vectors = []
    ids = result.get("ids", [])
    docs = result.get("documents", [])
    metas = result.get("metadatas", [])
    embeddings = result.get("embeddings", [])

    for i, vid in enumerate(ids):
        meta = metas[i] if i < len(metas) else {}
        doc = docs[i] if i < len(docs) else ""

        # 处理向量数值
        vec_values = []
        vec_preview = ""
        if i < len(embeddings) and embeddings[i] is not None:
            emb = embeddings[i]
            # 兼容 numpy 数组
            if hasattr(emb, 'tolist'):
                emb = emb.tolist()
            vec_values = [round(float(v), 6) for v in emb]
            # 预览：前 10 个值
            vec_preview = "[" + ", ".join(f"{v:.6f}" for v in vec_values[:10]) + ", ...]"

        vectors.append({
            "id": vid,
            "problemId": int(meta.get("problem_id", 0)),
            "problemNo": meta.get("problem_no", ""),
            "title": meta.get("title", ""),
            "tags": meta.get("tags", ""),
            "category": meta.get("category", ""),
            "difficulty": meta.get("difficulty", ""),
            "documentPreview": (doc[:200] if doc else ""),
            "vectorValues": vec_values,
            "vectorPreview": vec_preview,
            "vectorDimension": len(vec_values),
        })

    return {
        "total": total,
        "page": page,
        "pageSize": page_size,
        "totalPages": (total + page_size - 1) // page_size,
        "vectors": vectors,
    }


def get_collection_info() -> dict:
    """获取 Collection 完整信息"""
    col = get_collection()
    n = col.count()
    dim = get_dimension()
    metadata = col.metadata or {}
    return {
        "name": COLLECTION_NAME,
        "count": n,
        "dimension": dim,
        "distanceMetric": metadata.get("hnsw:space", "unknown"),
        "metadata": metadata,
        "chromaPath": CHROMA_PATH,
    }


def clear_all():
    """清空 Collection（危险操作，仅管理端调用）"""
    global _collection, _client
    client = get_client()
    try:
        client.delete_collection(COLLECTION_NAME)
    except Exception:
        pass
    _collection = None
    get_collection()  # 重新创建空 collection
    return True
