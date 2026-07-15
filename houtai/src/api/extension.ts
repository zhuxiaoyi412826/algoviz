import request from './request'
import type { Announcement, Feedback } from '@/types'

export const announcementApi = {
  getList: (params?: any) =>
    request.get<{ list: Announcement[]; total: number }>('/extension/announcement', { params }),

  create: (data: Partial<Announcement>) =>
    request.post<Announcement>('/extension/announcement', data),

  update: (id: string, data: Partial<Announcement>) =>
    request.put<Announcement>(`/extension/announcement/${id}`, data),

  delete: (id: string) =>
    request.delete<void>(`/extension/announcement/${id}`),

  publish: (id: string) =>
    request.post<void>(`/extension/announcement/${id}/publish`),

  toggleTop: (id: string) =>
    request.put<void>(`/extension/announcement/${id}/top`)
}

export const feedbackApi = {
  getList: (params?: any) =>
    request.get<{ list: Feedback[]; total: number }>('/extension/feedback', { params }),

  reply: (id: string, reply: string) =>
    request.put<Feedback>(`/extension/feedback/${id}/reply`, { reply }),

  updateStatus: (id: string, status: string) =>
    request.put<void>(`/extension/feedback/${id}/status`, { status })
}

export const backupApi = {
  getList: () =>
    request.get<any[]>('/extension/backup'),

  create: () =>
    request.post<{ id: string }>('/extension/backup'),

  restore: (id: string) =>
    request.post<void>(`/extension/backup/${id}/restore`),

  delete: (id: string) =>
    request.delete<void>(`/extension/backup/${id}`),

  download: (id: string) =>
    request.get<Blob>(`/extension/backup/${id}/download`, { responseType: 'blob' })
}

export const thirdPartyApi = {
  getConfig: () =>
    request.get<any>('/extension/third-party'),

  update: (data: any) =>
    request.put<void>('/extension/third-party', data)
}
