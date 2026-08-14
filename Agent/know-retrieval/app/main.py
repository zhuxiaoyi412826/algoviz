"""FastAPI 应用入口"""
import logging

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from .config import COLLECTION_NAME, EMBEDDING_MODEL, PORT
from .schemas import (
    BatchEmbedRequest,
    DeleteRequest,
    EmbedRequest,
    SearchRequest,
    SearchResponse,
    SearchResultItem,
    StatsResponse,
)
from . import vector_store

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
logger = logging.getLogger(__name__)

app = FastAPI(title="AlgoViz 向量检索服务", version="1.0.0", docs_url="/docs", redoc_url="/redoc")

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/")
async def root():
    """根路径 — 返回服务信息"""
    return {
        "service": "AlgoViz 向量检索服务",
        "version": "1.0.0",
        "docs": "/docs",
        "health": "/health",
        "endpoints": {
            "stats": "GET /api/v1/stats",
            "embed_single": "POST /api/v1/embedding/single",
            "embed_batch": "POST /api/v1/embedding/batch",
            "search": "POST /api/v1/search",
            "delete": "DELETE /api/v1/embedding/{problem_id}",
            "clear": "POST /api/v1/clear",
        },
    }


def build_text(p: EmbedRequest) -> str:
    """构造向量化文本 — 尽可能丰富语义信息"""
    parts = []
    if p.title:
        parts.append(f"题目：{p.title}")
    if p.category:
        parts.append(f"分类：{p.category}")
    if p.tags:
        parts.append(f"标签：{p.tags}")
    if p.difficulty:
        parts.append(f"难度：{p.difficulty}")
    if p.description:
        # 去除 Markdown 标记，保留纯文本，截取前 800 字
        desc = _strip_markdown(p.description)[:800]
        parts.append(f"描述：{desc}")
    if p.solution:
        # 题解包含关键思路和代码，截取前 500 字
        sol = _strip_markdown(p.solution)[:500]
        parts.append(f"题解：{sol}")
    return "\n".join(parts) if parts else (p.title or "")


def _strip_markdown(text: str) -> str:
    """简单去除 Markdown 标记，保留纯文本"""
    import re
    # 去除代码块
    text = re.sub(r'```[\s\S]*?```', '', text)
    # 去除行内代码
    text = re.sub(r'`([^`]+)`', r'\1', text)
    # 去除标题标记
    text = re.sub(r'^#{1,6}\s+', '', text, flags=re.MULTILINE)
    # 去除粗体/斜体
    text = re.sub(r'\*{1,3}([^*]+)\*{1,3}', r'\1', text)
    # 去除链接，保留文本
    text = re.sub(r'\[([^\]]+)\]\([^)]+\)', r'\1', text)
    # 去除图片
    text = re.sub(r'!\[([^\]]*)\]\([^)]+\)', '', text)
    # 去除 HTML 标签
    text = re.sub(r'<[^>]+>', '', text)
    # 压缩空白
    text = re.sub(r'\n{3,}', '\n\n', text).strip()
    return text


def build_metadata(p: EmbedRequest) -> dict:
    """构造 metadata"""
    return {
        "problem_id": p.problem_id,
        "problem_no": p.problem_no or "",
        "title": p.title or "",
        "tags": p.tags or "",
        "category": p.category or "",
        "difficulty": p.difficulty or "",
    }


# ===== 健康检查 =====
@app.get("/health")
async def health():
    """健康检查"""
    try:
        count = vector_store.count()
        return {
            "status": "ok",
            "collection": COLLECTION_NAME,
            "vector_count": count,
            "model": EMBEDDING_MODEL,
        }
    except Exception as e:
        return {"status": "error", "message": str(e)}


# ===== 统计 =====
@app.get("/api/v1/stats")
async def stats():
    """向量库统计信息"""
    try:
        count = vector_store.count()
        return {
            "collectionName": COLLECTION_NAME,
            "vectorCount": count,
            "modelName": EMBEDDING_MODEL,
            "status": "running",
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ===== Collection 完整信息（含维度、距离度量等）=====
@app.get("/api/v1/collection/info")
async def collection_info():
    """ChromaDB Collection 实时信息"""
    try:
        info = vector_store.get_collection_info()
        return {
            "name": info["name"],
            "vectorCount": info["count"],
            "dimension": info["dimension"],
            "distanceMetric": info["distanceMetric"],
            "metadata": info["metadata"],
            "chromaPath": info["chromaPath"],
            "modelName": EMBEDDING_MODEL,
        }
    except Exception as e:
        logger.error("Collection info 获取失败: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


# ===== 向量分页列表（含对应题目信息）=====
@app.get("/api/v1/vectors")
async def vectors_list(page: int = 1, pageSize: int = 50, keyword: str = ""):
    """分页查看向量库中的向量及其对应题目 metadata"""
    try:
        data = vector_store.get_vectors_page(page, pageSize)

        # 如果有 keyword，在当前页内过滤
        if keyword:
            kw = keyword.lower()
            filtered = [
                v for v in data["vectors"]
                if (v["title"] and kw in v["title"].lower())
                or (v["problemNo"] and kw in v["problemNo"].lower())
                or (v["tags"] and kw in v["tags"].lower())
                or (v["category"] and kw in v["category"].lower())
            ]
            data["vectors"] = filtered

        return data
    except Exception as e:
        logger.error("向量列表获取失败: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


# ===== 单条入库 =====
@app.post("/api/v1/embedding/single")
async def embed_single(req: EmbedRequest):
    """单条题目向量化入库"""
    try:
        text = build_text(req)
        if not text:
            raise HTTPException(status_code=400, detail="文本内容为空")
        meta = build_metadata(req)
        vector_store.upsert_problem(req.problem_id, text, meta)
        return {"success": True, "problem_id": req.problem_id}
    except HTTPException:
        raise
    except Exception as e:
        logger.error("单条入库失败: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


# ===== 批量入库（异步）=====
import asyncio
from concurrent.futures import ThreadPoolExecutor

_executor = ThreadPoolExecutor(max_workers=2)
_sync_task_status = {"running": False, "total": 0, "success": 0, "failed": 0, "message": ""}


def _do_batch_embed(items):
    """后台线程执行批量向量化"""
    global _sync_task_status
    _sync_task_status["running"] = True
    _sync_task_status["total"] = len(items)
    _sync_task_status["success"] = 0
    _sync_task_status["failed"] = 0
    _sync_task_status["message"] = "处理中..."
    try:
        result = vector_store.upsert_batch(items)
        _sync_task_status["success"] = result.get("success", 0)
        _sync_task_status["failed"] = result.get("failed", 0)
        _sync_task_status["message"] = f"完成：成功 {result.get('success', 0)}，失败 {result.get('failed', 0)}"
    except Exception as e:
        _sync_task_status["failed"] = len(items)
        _sync_task_status["message"] = f"失败: {str(e)}"
    finally:
        _sync_task_status["running"] = False


@app.post("/api/v1/embedding/batch")
async def embed_batch(req: BatchEmbedRequest):
    """批量题目向量化入库（异步提交）"""
    try:
        items = []
        for p in req.problems:
            text = build_text(p)
            if not text:
                continue
            items.append({"id": p.problem_id, "text": text, "metadata": build_metadata(p)})

        if not items:
            return {"success": 0, "failed": 0, "message": "无有效数据"}

        # 提交到后台线程池，立即返回
        loop = asyncio.get_running_loop()
        loop.run_in_executor(_executor, _do_batch_embed, items)

        return {"success": True, "message": f"已提交 {len(items)} 条题目到后台处理", "total": len(items)}
    except Exception as e:
        logger.error("批量入库提交失败: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


# ===== 同步任务状态 =====
@app.get("/api/v1/sync/status")
async def sync_status():
    """查询全量同步任务进度"""
    return _sync_task_status


# ===== 语义检索（混合检索：向量 + 关键词加权）=====
@app.post("/api/v1/search")
async def search(req: SearchRequest):
    """混合检索：向量语义召回 + 关键词匹配加权排序"""
    try:
        # 1. 向量召回 — 取更多候选（top_k * 3）以便后续重排
        candidate_k = max(req.top_k * 3, 30)
        raw_results = vector_store.search_by_text(req.query, candidate_k)

        if not raw_results:
            return {"results": [], "total": 0, "query": req.query}

        # 2. 关键词匹配加分
        query_lower = req.query.lower().strip()
        query_terms = [t for t in query_lower.replace(',', ' ').replace('，', ' ').split() if len(t) >= 1]

        scored_results = []
        for r in raw_results:
            vec_sim = r["similarity"]
            meta = r.get("metadata", {})

            # 关键词匹配加权
            kw_boost = 0.0
            if query_terms:
                title = (meta.get("title") or "").lower()
                tags = (meta.get("tags") or "").lower()
                category = (meta.get("category") or "").lower()
                doc = (r.get("document") or "").lower()

                for term in query_terms:
                    if term in title:
                        kw_boost += 0.15  # 标题命中权重最高
                    if term in tags:
                        kw_boost += 0.10  # 标签命中
                    if term in category:
                        kw_boost += 0.08  # 分类命中
                    if term in doc:
                        kw_boost += 0.05  # 正文命中

            # 混合得分 = 向量相似度(70%) + 关键词加权(30%)，上限 1.0
            hybrid_score = min(1.0, vec_sim * 0.7 + min(kw_boost, 0.3) * 1.0)

            # 阈值过滤（使用混合得分）
            if hybrid_score < req.threshold:
                continue

            scored_results.append({
                "problemId": int(meta.get("problem_id", 0)),
                "problemNo": meta.get("problem_no", ""),
                "title": meta.get("title", ""),
                "similarity": round(hybrid_score, 4),
                "vecSimilarity": round(vec_sim, 4),
                "kwBoost": round(kw_boost, 4),
                "category": meta.get("category", ""),
                "difficulty": meta.get("difficulty", ""),
                "tags": meta.get("tags", ""),
            })

        # 3. 按混合得分降序排列
        scored_results.sort(key=lambda x: x["similarity"], reverse=True)

        # 4. 截取 top_k
        scored_results = scored_results[:req.top_k]

        return {"results": scored_results, "total": len(scored_results), "query": req.query}
    except Exception as e:
        logger.error("检索失败: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


# ===== 删除向量 =====
@app.delete("/api/v1/embedding/{problem_id}")
async def delete_embedding(problem_id: int):
    """删除指定题目的向量"""
    try:
        vector_store.delete_problem(problem_id)
        return {"success": True, "problem_id": problem_id}
    except Exception as e:
        logger.error("删除失败: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


# ===== 清空向量库 =====
@app.post("/api/v1/clear")
async def clear():
    """清空向量库（管理操作）"""
    try:
        vector_store.clear_all()
        return {"success": True, "message": "向量库已清空"}
    except Exception as e:
        logger.error("清空失败: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=PORT)
