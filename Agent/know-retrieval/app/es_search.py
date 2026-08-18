"""Elasticsearch 面试题分词搜索模块

功能:
    1. 创建/管理面试题 ES 索引 (interview_problems)
    2. 将 MySQL 面试题数据同步到 ES
    3. 提供 ES 关键词分词搜索接口 (bool + multi_match + match_phrase)

与向量搜索的区别:
    - 向量搜索: 语义理解，同义词匹配，AI 能力
    - ES 搜索: 精确分词匹配，速度快，可高亮，适合标签/名称精准查找

中文分词策略:
    - 使用 IK Analyzer 分词器（用户已安装）
    - ik_smart: 粗粒度分词，适合搜索
    - ik_max_word: 细粒度分词，适合索引
    - 索引时用 ik_max_word（最大化召回），搜索时用 ik_smart（精确匹配）
"""

import logging
import time
from typing import List, Dict, Optional, Any

logger = logging.getLogger("algoviz")

# 延迟导入 elasticsearch，避免未安装时影响其他模块
_es_client = None


def get_es_client(es_url: str = "http://localhost:9200"):
    """获取 ES 单例客户端"""
    global _es_client
    if _es_client is None:
        try:
            from elasticsearch import Elasticsearch
            _es_client = Elasticsearch(
                es_url,
                timeout=10,
                max_retries=2,
                retry_on_timeout=True,
            )
            # 测试连接
            if not _es_client.ping():
                logger.warning("ES 连接失败: %s", es_url)
                _es_client = None
                return None
            logger.info("ES 客户端初始化成功: %s", es_url)
        except ImportError:
            logger.warning("elasticsearch-py 未安装, 请执行: pip install elasticsearch>=7.0.0,<8.0.0")
            _es_client = None
        except Exception as e:
            logger.error("ES 客户端初始化失败: %s", e)
            _es_client = None
    return _es_client


# 面试题索引名
ES_INDEX = "interview_problems"

# 索引 Mapping + Settings（使用 IK 分词器）
INDEX_SETTINGS = {
    "settings": {
        "number_of_shards": 1,
        "number_of_replicas": 0,
        "analysis": {
            "analyzer": {
                # 索引时用 ik_max_word：细粒度切分，最大化召回
                "ik_max_word_analyzer": {
                    "type": "custom",
                    "tokenizer": "ik_max_word",
                    "filter": ["lowercase"]
                },
                # 搜索时用 ik_smart：粗粒度切分，减少噪音，提高精度
                "ik_smart_analyzer": {
                    "type": "custom",
                    "tokenizer": "ik_smart",
                    "filter": ["lowercase"]
                }
            }
        }
    },
    "mappings": {
        "properties": {
            "problem_id": {
                "type": "integer"
            },
            "problem_no": {
                "type": "keyword"
            },
            "title": {
                "type": "text",
                "analyzer": "ik_max_word_analyzer",
                "search_analyzer": "ik_smart_analyzer",
                "fields": {
                    "keyword": {
                        "type": "keyword",
                        "ignore_above": 256
                    }
                }
            },
            "tags": {
                "type": "text",
                "analyzer": "ik_max_word_analyzer",
                "search_analyzer": "ik_smart_analyzer",
                "fields": {
                    "keyword": {
                        "type": "keyword"
                    }
                }
            },
            "category": {
                "type": "text",
                "analyzer": "ik_max_word_analyzer",
                "search_analyzer": "ik_smart_analyzer",
                "fields": {
                    "keyword": {
                        "type": "keyword"
                    }
                }
            },
            "difficulty": {
                "type": "keyword"
            },
            "description": {
                "type": "text",
                "analyzer": "ik_max_word_analyzer",
                "search_analyzer": "ik_smart_analyzer"
            },
            "content": {
                "type": "text",
                "analyzer": "ik_max_word_analyzer",
                "search_analyzer": "ik_smart_analyzer"
            },
            "view_count": {
                "type": "integer"
            },
            "created_at": {
                "type": "date",
                "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
            }
        }
    }
}


def ensure_index(es_client, index_name: str = ES_INDEX) -> bool:
    """确保面试题索引存在，不存在则创建（使用 IK 分词器）"""
    try:
        if es_client.indices.exists(index=index_name):
            logger.info("ES 索引 %s 已存在", index_name)
            return True

        es_client.indices.create(index=index_name, body=INDEX_SETTINGS)
        logger.info("ES 索引 %s 创建成功（IK 分词器）", index_name)
        return True
    except Exception as e:
        logger.error("ES 索引 %s 创建失败: %s", index_name, e)
        return False


def recreate_index(es_client, index_name: str = ES_INDEX) -> bool:
    """删除并重建索引（用于切换分词器或 mapping 变更）"""
    try:
        if es_client.indices.exists(index=index_name):
            es_client.indices.delete(index=index_name)
            logger.info("ES 旧索引 %s 已删除", index_name)
        es_client.indices.create(index=index_name, body=INDEX_SETTINGS)
        logger.info("ES 索引 %s 重建成功（IK 分词器）", index_name)
        return True
    except Exception as e:
        logger.error("ES 索引 %s 重建失败: %s", index_name, e)
        return False


def index_problem(es_client, problem: Dict[str, Any]) -> bool:
    """单条题目写入 ES"""
    try:
        doc = {
            "problem_id": problem.get("id"),
            "problem_no": problem.get("problemNo", ""),
            "title": problem.get("title", ""),
            "tags": problem.get("tags", ""),
            "category": problem.get("category", ""),
            "difficulty": problem.get("difficulty", ""),
            "description": problem.get("description", ""),
            "content": problem.get("content", ""),
            "view_count": problem.get("viewCount", 0),
        }
        created_at = problem.get("createdAt")
        if created_at:
            doc["created_at"] = created_at
        es_client.index(
            index=ES_INDEX,
            id=str(problem.get("id")),
            body=doc,
            refresh=True,
        )
        return True
    except Exception as e:
        logger.error("ES 索引写入失败: id=%s error=%s", problem.get("id"), e)
        return False


def bulk_index_problems(es_client, problems: List[Dict[str, Any]]) -> Dict[str, int]:
    """批量写入题目到 ES"""
    success = 0
    failed = 0
    try:
        actions = []
        for p in problems:
            doc = {
                "problem_id": p.get("id"),
                "problem_no": p.get("problemNo", ""),
                "title": p.get("title", ""),
                "tags": p.get("tags", ""),
                "category": p.get("category", ""),
                "difficulty": p.get("difficulty", ""),
                "description": p.get("description", ""),
                "content": p.get("content", ""),
                "view_count": p.get("viewCount", 0),
            }
            # 仅在日期有效时添加，避免 ES date 类型解析 null / 空字符串失败
            created_at = p.get("createdAt")
            if created_at:
                doc["created_at"] = created_at
            actions.append({"index": {"_index": ES_INDEX, "_id": str(p.get("id"))}})
            actions.append(doc)

        if actions:
            result = es_client.bulk(body=actions, refresh=True)
            if result.get("errors"):
                for item in result.get("items", []):
                    if item.get("index", {}).get("status", 0) < 400:
                        success += 1
                    else:
                        failed += 1
                # 记录第一条失败原因，便于排查（日期格式/字段类型不匹配等）
                for item in result.get("items", []):
                    err = item.get("index", {}).get("error")
                    if err:
                        logger.error("ES bulk 首条失败详情: id=%s type=%s reason=%s",
                                     item.get("index", {}).get("_id"),
                                     err.get("type"), err.get("reason"))
                        break
            else:
                success = len(problems)

        return {"success": success, "failed": failed}
    except Exception as e:
        logger.error("ES 批量索引失败: %s", e)
        return {"success": 0, "failed": len(problems)}


def search_problems(es_client, query: str, top_k: int = 20, difficulty: str = "") -> Dict[str, Any]:
    """
    ES 关键词分词搜索（使用 IK 分词器）

    查询策略:
        - bool query: should (multi_match) + filter
        - multi_match: title^3 + tags^2 + category^1.5 + description^1 + content^0.5
        - match_phrase: 精确短语匹配加分
        - filter: difficulty 过滤 (不参与评分)
        - highlight: 关键词高亮

    返回:
        {
            "results": [...],
            "total": N,
            "query": "搜索词",
            "took_ms": 123,
            "highlight": true
        }
    """
    start_time = time.time()

    # 构建查询（IK 分词器在 mapping 中已配置，这里直接用 match 即可自动调用 ik_smart）
    should_queries = [
        # 标题精确匹配 (最高分)
        {
            "match_phrase": {
                "title": {
                    "query": query,
                    "boost": 4.0
                }
            }
        },
        # 标题分词匹配
        {
            "match": {
                "title": {
                    "query": query,
                    "boost": 3.0,
                    "operator": "or"
                }
            }
        },
        # 标签精确匹配
        {
            "match_phrase": {
                "tags": {
                    "query": query,
                    "boost": 3.0
                }
            }
        },
        # 标签分词匹配
        {
            "match": {
                "tags": {
                    "query": query,
                    "boost": 2.0
                }
            }
        },
        # 分类匹配
        {
            "match": {
                "category": {
                    "query": query,
                    "boost": 1.5
                }
            }
        },
        # 描述匹配
        {
            "match": {
                "description": {
                    "query": query,
                    "boost": 1.0
                }
            }
        },
        # 内容匹配
        {
            "match": {
                "content": {
                    "query": query,
                    "boost": 0.5
                }
            }
        },
    ]

    # 过滤条件
    filter_queries = []
    if difficulty:
        filter_queries.append({
            "term": {"difficulty": difficulty}
        })

    body = {
        "query": {
            "bool": {
                "should": should_queries,
                "filter": filter_queries,
                "minimum_should_match": 1
            }
        },
        "highlight": {
            "fields": {
                "title": {},
                "tags": {},
                "description": {},
                "category": {}
            },
            "pre_tags": ["<em>"],
            "post_tags": ["</em>"],
            "fragment_size": 150
        },
        "from": 0,
        "size": top_k,
        "_source": ["problem_id", "problem_no", "title", "tags", "category", "difficulty"]
    }

    try:
        result = es_client.search(index=ES_INDEX, body=body)
        hits = result.get("hits", {}).get("hits", [])
        total = result.get("hits", {}).get("total", {}).get("value", 0)

        results = []
        for hit in hits:
            source = hit.get("_source", {})
            highlight = hit.get("highlight", {})
            results.append({
                "problemId": source.get("problem_id"),
                "problemNo": source.get("problem_no", ""),
                "title": source.get("title", ""),
                "tags": source.get("tags", ""),
                "category": source.get("category", ""),
                "difficulty": source.get("difficulty", ""),
                "score": round(hit.get("_score", 0), 4),
                "highlight": highlight,
            })

        took_ms = int((time.time() - start_time) * 1000)

        return {
            "results": results,
            "total": total,
            "query": query,
            "took_ms": took_ms,
        }

    except Exception as e:
        logger.error("ES 搜索失败: query='%s' error=%s", query, e)
        raise


def get_index_stats(es_client) -> Dict[str, Any]:
    """获取 ES 索引统计信息"""
    try:
        if not es_client.indices.exists(index=ES_INDEX):
            return {"exists": False, "count": 0, "index": ES_INDEX}

        count = es_client.count(index=ES_INDEX)
        # 获取 mapping 信息（判断分词器是否为 IK）
        mapping = es_client.indices.get_mapping(index=ES_INDEX)
        index_mapping = mapping.get(ES_INDEX, {}).get("mappings", {})

        return {
            "exists": True,
            "count": count.get("count", 0),
            "index": ES_INDEX,
            "mapping": index_mapping,
            "analyzer": "ik_max_word (索引) / ik_smart (搜索)",
        }
    except Exception as e:
        logger.error("ES 索引统计失败: %s", e)
        return {"exists": False, "count": 0, "error": str(e)}


def delete_problem_from_index(es_client, problem_id: int) -> bool:
    """从 ES 索引中删除单条题目"""
    try:
        if es_client.indices.exists(index=ES_INDEX):
            es_client.delete(index=ES_INDEX, id=str(problem_id), refresh=True)
            logger.info("ES 索引删除成功: id=%s", problem_id)
        return True
    except Exception as e:
        logger.warning("ES 索引删除失败: id=%s err=%s", problem_id, e)
        return False


def delete_index(es_client) -> bool:
    """删除面试题索引"""
    try:
        if es_client.indices.exists(index=ES_INDEX):
            es_client.indices.delete(index=ES_INDEX)
            logger.info("ES 索引 %s 已删除", ES_INDEX)
            return True
        return False
    except Exception as e:
        logger.error("ES 索引删除失败: %s", e)
        return False


def test_ik_analyzer(es_client, text: str = "动态规划入门二叉树遍历") -> Dict[str, Any]:
    """测试 IK 分词器是否生效"""
    try:
        # 方式1: 测试索引内自定义分析器（需指定 index）
        body = {"text": text}
        try:
            result = es_client.indices.analyze(index=ES_INDEX, body=body, analyzer="ik_smart_analyzer")
            tokens = [t.get("token") for t in result.get("tokens", [])]
            if tokens:
                return {
                    "input": text,
                    "tokens": tokens,
                    "analyzer": "ik_smart_analyzer (interview_problems)",
                    "success": True
                }
        except Exception:
            pass

        # 方式2: 直接用全局 ik_smart tokenizer
        body = {"text": text, "tokenizer": "ik_smart"}
        result = es_client.indices.analyze(body=body)
        tokens = [t.get("token") for t in result.get("tokens", [])]
        return {
            "input": text,
            "tokens": tokens,
            "analyzer": "ik_smart (全局 tokenizer)",
            "success": len(tokens) > 0
        }
    except Exception as e:
        return {
            "input": text,
            "tokens": [],
            "error": str(e),
            "success": False
        }
