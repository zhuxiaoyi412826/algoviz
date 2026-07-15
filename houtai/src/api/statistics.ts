import request from './request'

export const statisticsApi = {
  getCoreMetrics: (params?: { startDate?: string; endDate?: string }) =>
    request.get<any>('/statistics/core-metrics', { params }),

  getOJAnalysis: (params?: { startDate?: string; endDate?: string }) =>
    request.get<any>('/statistics/oj-analysis', { params }),

  getVisualizationStats: () =>
    request.get<any>('/statistics/visualization'),

  exportData: (params: { type: string; startDate: string; endDate: string; format: string }) =>
    request.get<Blob>('/statistics/export', { params, responseType: 'blob' })
}

export const ojStatsApi = {
  getSubmissionTrend: (params?: { days?: number }) =>
    request.get<any[]>('/statistics/oj/trend', { params }),

  getDifficultyStats: () =>
    request.get<any>('/statistics/oj/difficulty'),

  getLanguageStats: () =>
    request.get<any[]>('/statistics/oj/language'),

  getErrorTypeStats: () =>
    request.get<any[]>('/statistics/oj/error-type')
}
