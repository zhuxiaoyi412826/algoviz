import request from './request'
import type { ServiceStatus } from '@/types'

export const monitorApi = {
  getServiceStatus: () =>
    request.get<ServiceStatus[]>('/monitor/service'),

  getResourceUsage: () =>
    request.get<any>('/monitor/resource'),

  getResponseTime: (params?: { hours?: number }) =>
    request.get<any[]>('/monitor/response-time', { params }),

  getDBStatus: () =>
    request.get<any>('/monitor/database')
}

export const alarmApi = {
  getList: (params?: any) =>
    request.get<{ list: any[]; total: number }>('/monitor/alarm', { params }),

  getConfig: () =>
    request.get<any>('/monitor/alarm/config'),

  updateConfig: (data: any) =>
    request.put<void>('/monitor/alarm/config', data),

  markRead: (id: string) =>
    request.put<void>(`/monitor/alarm/${id}/read`)
}

export const logApi = {
  getList: (params: { type: string; startTime?: string; endTime?: string; keyword?: string; page?: number; pageSize?: number }) =>
    request.get<{ list: any[]; total: number }>('/monitor/log', { params }),

  getTypes: () =>
    request.get<string[]>('/monitor/log/types'),

  download: (params: { type: string; startTime: string; endTime: string }) =>
    request.get<Blob>('/monitor/log/download', { params, responseType: 'blob' })
}
