"""FastAPI 应用入口"""
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from .config import COLLECTION_NAME, EMBEDDING_MODEL, PORT, ES_URL
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
from . import es_search

# ===== 全链路追踪：初始化日志 + 中间件 =====
from .trace_middleware import TraceIdMiddleware, setup_logging, get_trace_id

logger = setup_logging()  # 配置日志（控制台 + 文件，自动携带 TraceId）

app = FastAPI(title="AlgoViz 向量检索服务", version="1.0.0", docs_url="/docs", redoc_url="/redoc")

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# TraceId 全链路追踪中间件（必须在 CORS 之后，最早执行）
app.add_middleware(TraceIdMiddleware)


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
        logger.info("单条入库成功: problem_id=%s title=%s", req.problem_id, req.title)
        return {"success": True, "problem_id": req.problem_id}
    except HTTPException:
        raise
    except Exception as e:
        logger.error("单条入库失败: problem_id=%s error=%s", req.problem_id, e, exc_info=True)
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
        trace_id = get_trace_id()
        logger.info("语义搜索开始: query='%s' top_k=%s threshold=%s", req.query, req.top_k, req.threshold)

        # 1. 向量召回 — 取更多候选（top_k * 3）以便后续重排
        candidate_k = max(req.top_k * 3, 30)
        raw_results = vector_store.search_by_text(req.query, candidate_k)
        logger.info("向量召回完成: 候选数=%s", len(raw_results) if raw_results else 0)

        if not raw_results:
            logger.info("语义搜索无结果: query='%s'", req.query)
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
                        kw_boost += 0.15
                    if term in tags:
                        kw_boost += 0.10
                    if term in category:
                        kw_boost += 0.08
                    if term in doc:
                        kw_boost += 0.05

            # 混合得分 = 向量相似度(70%) + 关键词加权(30%)
            hybrid_score = min(1.0, vec_sim * 0.7 + min(kw_boost, 0.3) * 1.0)

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

        logger.info("语义搜索完成: query='%s' 返回=%s 条 最高相似度=%.4f",
                     req.query, len(scored_results),
                     scored_results[0]["similarity"] if scored_results else 0)

        return {"results": scored_results, "total": len(scored_results), "query": req.query}
    except Exception as e:
        logger.error("检索失败: query='%s' error=%s", req.query, e, exc_info=True)
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


# ============================================================
# ===== Elasticsearch 分词搜索 + 索引管理接口 =====
# ============================================================

class ESSearchRequest(BaseModel):
    """ES 分词搜索请求"""
    query: str = Field(..., description="搜索关键词")
    top_k: int = Field(20, alias="topK", description="返回数量")
    difficulty: str = Field("", description="难度过滤: easy/medium/hard")

    model_config = {"populate_by_name": True}


class ESIndexSingleRequest(BaseModel):
    """单条题目写入 ES 索引（接受 Java 驼峰字段）"""
    id: int
    problem_no: str = Field("", alias="problemNo")
    title: str = ""
    tags: str = ""
    category: str = ""
    difficulty: str = ""
    description: str = ""
    content: str = ""
    view_count: int = Field(0, alias="viewCount")
    created_at: str = Field("", alias="createdAt")

    model_config = {"populate_by_name": True}


class ESIndexBatchRequest(BaseModel):
    """批量题目写入 ES 索引"""
    problems: list[dict] = Field(..., description="题目列表（每项字段同 ESIndexSingleRequest）")


@app.get("/api/v1/es/health")
async def es_health():
    """ES 服务健康检查"""
    try:
        es = es_search.get_es_client(ES_URL)
        if es is None:
            return {"status": "offline", "message": "ES 客户端未初始化"}
        return {"status": "ok", "es_url": ES_URL, "index": es_search.ES_INDEX}
    except Exception as e:
        return {"status": "error", "message": str(e)}


@app.post("/api/v1/es-search")
async def es_search_problems(req: ESSearchRequest):
    """ES 关键词分词搜索（IK 分词器）"""
    try:
        trace_id = get_trace_id()
        logger.info("ES 分词搜索: query='%s' top_k=%s difficulty=%s",
                     req.query, req.top_k, req.difficulty)

        es = es_search.get_es_client(ES_URL)
        if es is None:
            raise HTTPException(status_code=503, detail="ES 服务不可用，请检查 Elasticsearch 是否启动")

        # 确保索引存在
        es_search.ensure_index(es)

        # 执行搜索
        result = es_search.search_problems(es, req.query, req.top_k, req.difficulty)
        logger.info("ES 搜索完成: query='%s' 返回=%s 条 耗时=%sms",
                     req.query, result.get("total", 0), result.get("took_ms", 0))
        return result
    except HTTPException:
        raise
    except Exception as e:
        logger.error("ES 搜索失败: query='%s' error=%s", req.query, e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/es/stats")
async def es_stats():
    """ES 索引统计信息"""
    try:
        es = es_search.get_es_client(ES_URL)
        if es is None:
            return {"exists": False, "count": 0, "message": "ES 服务不可用"}
        return es_search.get_index_stats(es)
    except Exception as e:
        logger.error("ES 统计失败: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/es/index/single")
async def es_index_single(req: ESIndexSingleRequest):
    """单条题目写入 ES 索引（题目新增/修改时自动调用）"""
    try:
        es = es_search.get_es_client(ES_URL)
        if es is None:
            raise HTTPException(status_code=503, detail="ES 服务不可用")

        es_search.ensure_index(es)
        problem = {
            "id": req.id,
            "problemNo": req.problem_no,
            "title": req.title,
            "tags": req.tags,
            "category": req.category,
            "difficulty": req.difficulty,
            "description": req.description,
            "content": req.content,
            "viewCount": req.view_count,
            "createdAt": req.created_at,
        }
        ok = es_search.index_problem(es, problem)
        if ok:
            logger.info("ES 单条索引成功: id=%s title=%s", req.id, req.title)
            return {"success": True, "id": req.id}
        else:
            raise HTTPException(status_code=500, detail="索引写入失败")
    except HTTPException:
        raise
    except Exception as e:
        logger.error("ES 单条索引失败: id=%s error=%s", req.id, e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/es/index/batch")
async def es_index_batch(req: ESIndexBatchRequest):
    """批量题目写入 ES 索引"""
    try:
        es = es_search.get_es_client(ES_URL)
        if es is None:
            raise HTTPException(status_code=503, detail="ES 服务不可用")

        es_search.ensure_index(es)
        result = es_search.bulk_index_problems(es, req.problems)
        logger.info("ES 批量索引完成: success=%s failed=%s",
                     result.get("success", 0), result.get("failed", 0))
        return result
    except HTTPException:
        raise
    except Exception as e:
        logger.error("ES 批量索引失败: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.delete("/api/v1/es/index/{problem_id}")
async def es_delete_index(problem_id: int):
    """从 ES 索引中删除单条题目（题目删除时自动调用）"""
    try:
        es = es_search.get_es_client(ES_URL)
        if es is None:
            return {"success": False, "message": "ES 服务不可用"}
        ok = es_search.delete_problem_from_index(es, problem_id)
        return {"success": ok, "id": problem_id}
    except Exception as e:
        logger.error("ES 索引删除失败: id=%s error=%s", problem_id, e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/es/index/recreate")
async def es_recreate_index():
    """删除并重建 ES 索引（切换分词器或 mapping 变更时使用）"""
    try:
        es = es_search.get_es_client(ES_URL)
        if es is None:
            raise HTTPException(status_code=503, detail="ES 服务不可用")
        ok = es_search.recreate_index(es)
        return {"success": ok, "message": "索引已重建（使用 IK 分词器）" if ok else "索引重建失败"}
    except HTTPException:
        raise
    except Exception as e:
        logger.error("ES 索引重建失败: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.delete("/api/v1/es/index")
async def es_delete_index_all():
    """删除整个面试题索引"""
    try:
        es = es_search.get_es_client(ES_URL)
        if es is None:
            return {"success": False, "message": "ES 服务不可用"}
        ok = es_search.delete_index(es)
        return {"success": ok, "message": "索引已删除" if ok else "索引不存在或删除失败"}
    except Exception as e:
        logger.error("ES 索引删除失败: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/es/test-analyzer")
async def es_test_analyzer(text: str = "动态规划入门二叉树遍历"):
    """测试 IK 分词器分词效果"""
    try:
        es = es_search.get_es_client(ES_URL)
        if es is None:
            raise HTTPException(status_code=503, detail="ES 服务不可用")
        return es_search.test_ik_analyzer(es, text)
    except HTTPException:
        raise
    except Exception as e:
        logger.error("IK 分词测试失败: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=PORT)
