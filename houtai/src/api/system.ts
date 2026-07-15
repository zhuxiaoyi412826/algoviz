import request from './request'
import type {
  User,
  LoginLog,
  OperationLog,
  SystemConfig
} from '@/types'

export const adminApi = {
  getList: (params?: any) =>
    request.get<{ list: User[]; total: number }>('/system/admin', { params }),

  create: (data: Partial<User>) =>
    request.post<User>('/system/admin', data),

  update: (id: string, data: Partial<User>) =>
    request.put<User>(`/system/admin/${id}`, data),

  delete: (id: string) =>
    request.delete<void>(`/system/admin/${id}`),

  resetPassword: (id: string) =>
    request.post<void>(`/system/admin/${id}/reset-password`),

  changeStatus: (id: string, status: string) =>
    request.put<void>(`/system/admin/${id}/status`, { status })
}

export const loginLogApi = {
  getList: (params?: any) =>
    request.get<{ list: LoginLog[]; total: number }>('/system/login-log', { params }),

  getStats: () =>
    request.get<{ todayCount: number; weekCount: number; failCount: number }>('/system/login-log/stats')
}

export const operationLogApi = {
  getList: (params?: any) =>
    request.get<{ list: OperationLog[]; total: number }>('/system/operation-log', { params }),

  getDetail: (id: string) =>
    request.get<OperationLog>(`/system/operation-log/${id}`),

  export: (params: any) =>
    request.get<Blob>('/system/operation-log/export', { params, responseType: 'blob' })
}

export const systemConfigApi = {
  getAll: () =>
    request.get<SystemConfig[]>('/system/config'),

  update: (data: Record<string, any>) =>
    request.put<void>('/system/config', data)
}
