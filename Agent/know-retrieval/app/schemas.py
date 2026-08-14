"""请求/响应数据模型"""
from pydantic import BaseModel, Field


class EmbedRequest(BaseModel):
    """单条题目向量化入库（兼容 Java 驼峰命名）"""
    problem_id: int = Field(..., alias="problemId", description="题目 ID")
    title: str = Field("", description="标题")
    tags: str = Field("", description="标签")
    category: str = Field("", description="分类")
    difficulty: str = Field("", description="难度")
    description: str = Field("", description="题目描述")
    solution: str = Field("", description="题解")
    problem_no: str = Field("", alias="problemNo", description="题目编号")

    model_config = {"populate_by_name": True}


class BatchEmbedRequest(BaseModel):
    """批量向量化入库"""
    problems: list[EmbedRequest] = Field(..., description="题目列表")


class SearchRequest(BaseModel):
    """语义检索请求"""
    query: str = Field(..., description="搜索文本")
    top_k: int = Field(20, alias="topK", description="返回数量")
    threshold: float = Field(0.3, description="混合得分阈值")

    model_config = {"populate_by_name": True}


class SearchResultItem(BaseModel):
    """单条检索结果"""
    problem_id: int = Field(..., alias="problemId")
    problem_no: str = Field("", alias="problemNo")
    title: str = ""
    similarity: float
    category: str = ""
    difficulty: str = ""
    tags: str = ""

    model_config = {"populate_by_name": True}


class SearchResponse(BaseModel):
    """检索响应"""
    results: list[SearchResultItem]
    total: int
    query: str


class DeleteRequest(BaseModel):
    """删除请求"""
    problem_id: int = Field(..., alias="problemId")

    model_config = {"populate_by_name": True}


class StatsResponse(BaseModel):
    """向量库统计"""
    collection_name: str
    vector_count: int
    model_name: str
    status: str = "running"
