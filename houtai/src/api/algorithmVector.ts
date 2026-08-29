import request from './request'

// 算法题向量管理接口（Qdrant）
// 注：request 响应拦截器已统一返回 res.data ?? res（运行时数据已解包），
// 此处显式标注 Promise<any> 以对齐运行时行为，避免调用方收到 AxiosResponse 类型。
export const algorithmVectorApi = {
  // 健康检查
  health: (): Promise<any> =>
    request.get<any>('/algorithm-vector/admin/health'),

  // 向量库统计（精简版）
  stats: (): Promise<any> =>
    request.get<any>('/algorithm-vector/admin/stats'),

  // 全量同步（异步提交）
  syncAll: (): Promise<any> =>
    request.post<any>('/algorithm-vector/admin/sync'),

  // 同步任务进度（轮询用）
  syncProgress: (): Promise<any> =>
    request.get<any>('/algorithm-vector/admin/sync/progress'),

  // 取消同步
  cancelSync: (): Promise<any> =>
    request.post<any>('/algorithm-vector/admin/sync/cancel'),

  // 清空向量库
  clear: (): Promise<any> =>
    request.post<any>('/algorithm-vector/admin/clear'),

  // Collection 完整信息（当前接口与 stats 同源，返回 collectionName/dim/distance/modelName 等）
  collectionInfo: (): Promise<any> =>
    request.get<any>('/algorithm-vector/admin/stats'),

  // 向量分页列表（含对应算法题信息；支持组合过滤）
  vectors: (params: {
    page?: number
    pageSize?: number
    keyword?: string
    algorithmId?: string
    category?: string
    tags?: string
  }): Promise<any> =>
    request.get<any>('/algorithm-vector/admin/vectors', { params }),

  // 语义搜索
  search: (params: { query: string; topK?: number; category?: string }): Promise<any> =>
    request.post<any>('/algorithm-vector/admin/search', params),

  // 删除单个算法题向量
  delete: (algorithmId: number | string): Promise<any> =>
    request.delete<any>(`/algorithm-vector/admin/vectors/${algorithmId}`)
}
