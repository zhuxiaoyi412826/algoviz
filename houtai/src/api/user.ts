import request from './request'
import type { AppUser, Statistics } from '@/types'

export const userApi = {
  getList: (params?: any) =>
    request.get<{ list: AppUser[]; total: number }>('/user/list', { params }),

  getDetail: (id: string) =>
    request.get<AppUser>(`/user/${id}`),

  update: (id: string, data: Partial<AppUser>) =>
    request.put<AppUser>(`/user/${id}`, data),

  changeStatus: (id: string, status: string) =>
    request.put<void>(`/user/${id}/status`, { status }),

  getLoginRecords: (userId: string, params?: any) =>
    request.get<{ list: any[]; total: number }>(`/user/${userId}/login-records`, { params }),

  getBehaviorStats: (userId: string) =>
    request.get<any>(`/user/${userId}/behavior`)
}

export const userStatsApi = {
  getOverview: () =>
    request.get<Statistics>('/user/stats/overview'),

  getTrend: (params: { startDate: string; endDate: string }) =>
    request.get<Statistics[]>('/user/stats/trend', { params }),

  getBehaviorDistribution: () =>
    request.get<{ dsVisits: number; algoVisits: number; ojVisits: number; aiDialogues: number }>('/user/stats/behavior')
}
