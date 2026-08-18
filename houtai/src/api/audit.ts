import request from './request'

// 关键词屏蔽审核系统接口
export const auditApi = {
  // ===== 敏感词 =====
  words: (params: { keyword?: string; category?: string; level?: string; page?: number; pageSize?: number }) =>
    request.get<any>('/audit/words', { params }),

  saveWord: (data: any) =>
    request.post<any>('/audit/words', data),

  saveWordsBatch: (words: any[]) =>
    request.post<any>('/audit/words/batch', { words }),

  deleteWord: (id: number) =>
    request.delete<any>(`/audit/words/${id}`),

  deleteWordsBatch: (ids: number[]) =>
    request.delete<any>('/audit/words/batch', { data: { ids } }),

  refreshCache: () =>
    request.post<any>('/audit/words/refresh-cache'),

  // ===== 版本 =====
  versions: () =>
    request.get<any>('/audit/versions'),

  publishVersion: (remark?: string) =>
    request.post<any>('/audit/versions/publish', { remark }),

  rollbackVersion: (versionNo: number) =>
    request.post<any>(`/audit/versions/${versionNo}/rollback`),

  // ===== 危险代码规则 =====
  rules: (params: { keyword?: string; language?: string; riskLevel?: string; page?: number; pageSize?: number }) =>
    request.get<any>('/audit/rules', { params }),

  saveRule: (data: any) =>
    request.post<any>('/audit/rules', data),

  deleteRule: (id: number) =>
    request.delete<any>(`/audit/rules/${id}`),

  // ===== 检测测试 =====
  detect: (data: { title?: string; content: string; language?: string; contentType?: string }) =>
    request.post<any>('/audit/detect', data),

  // ===== 待审核 + 人工审核 =====
  pending: (params: { page?: number; pageSize?: number }) =>
    request.get<any>('/audit/pending', { params }),

  manualPull: () =>
    request.post<any>('/audit/pending/pull'),

  review: (data: { submitId: string; result: 'pass' | 'reject'; remark?: string }) =>
    request.post<any>('/audit/review', data),

  records: (params: { auditStatus?: string; riskLevel?: string; keyword?: string; page?: number; pageSize?: number }) =>
    request.get<any>('/audit/records', { params }),

  stats: () =>
    request.get<any>('/audit/stats'),
}
