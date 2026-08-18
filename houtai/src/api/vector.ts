import request from './request'

// 向量数据库管理接口
export const vectorApi = {
  // 健康检查
  health: () =>
    request.get<any>('/vector/admin/health'),

  // 向量库统计（精简版）
  stats: () =>
    request.get<any>('/vector/admin/stats'),

  // 全量同步
  syncAll: () =>
    request.post<any>('/vector/admin/sync-all'),

  // 同步任务进度（轮询用）
  syncProgress: () =>
    request.get<any>('/vector/admin/sync/progress'),

  // 清空向量库
  clear: () =>
    request.post<any>('/vector/admin/clear'),

  // 用户端语义搜索
  search: (params: { query: string; topK?: number }) =>
    request.get<any>('/vector/search', { params }),

  // ===== ChromaDB 实时检测 =====
  // Collection 完整信息（向量数、维度、距离度量、模型名）
  collectionInfo: () =>
    request.get<any>('/vector/admin/collection-info'),

  // 向量分页列表（含对应题目信息）
  vectors: (params: { page?: number; pageSize?: number; keyword?: string }) =>
    request.get<any>('/vector/admin/vectors', { params }),

  // ===== Elasticsearch 分词索引管理 =====
  // ES 健康检查
  esHealth: () =>
    request.get<any>('/vector/admin/es/health'),

  // ES 索引统计
  esStats: () =>
    request.get<any>('/vector/admin/es/stats'),

  // 全量同步面试题到 ES 索引
  esSyncAll: () =>
    request.post<any>('/vector/admin/es/sync-all'),

  // 重建 ES 索引（删除并重建）
  esRecreate: () =>
    request.post<any>('/vector/admin/es/recreate'),

  // 删除整个 ES 索引
  esDeleteIndex: () =>
    request.delete<any>('/vector/admin/es/index'),

  // 测试 IK 分词器
  esTestAnalyzer: (text: string) =>
    request.get<any>('/vector/admin/es/test-analyzer', { params: { text } }),
}
