import request from './request'
import type {
  DataStructure,
  Algorithm,
  OJProblem,
  InterviewProblem,
  TestCase,
  AIPrompt
} from '@/types'

export const dataStructureApi = {
  getList: (params?: any) =>
    request.get<{ list: DataStructure[]; total: number }>('/content/datastructure', { params }),

  create: (data: Partial<DataStructure>) =>
    request.post<DataStructure>('/content/datastructure', data),

  update: (id: string, data: Partial<DataStructure>) =>
    request.put<DataStructure>(`/content/datastructure/${id}`, data),

  delete: (id: string) =>
    request.delete<void>(`/content/datastructure/${id}`)
}

export const algorithmApi = {
  getList: (params?: any) =>
    request.get<{ list: Algorithm[]; total: number }>('/content/algorithm', { params }),

  create: (data: Partial<Algorithm>) =>
    request.post<Algorithm>('/content/algorithm', data),

  update: (id: string, data: Partial<Algorithm>) =>
    request.put<Algorithm>(`/content/algorithm/${id}`, data),

  delete: (id: string) =>
    request.delete<void>(`/content/algorithm/${id}`)
}

export const problemApi = {
  getList: (params?: any) =>
    request.get<{ list: OJProblem[]; total: number }>('/content/problem', { params }),

  create: (data: Partial<OJProblem>) =>
    request.post<OJProblem>('/content/problem', data),

  update: (id: string, data: Partial<OJProblem>) =>
    request.put<OJProblem>(`/content/problem/${id}`, data),

  delete: (id: string) =>
    request.delete<void>(`/content/problem/${id}`),

  changeStatus: (id: string, status: string) =>
    request.put<void>(`/content/problem/${id}/status`, { status })
}

export const testCaseApi = {
  getByProblem: (problemId: string) =>
    request.get<TestCase[]>(`/content/testcase/problem/${problemId}`),

  create: (data: Partial<TestCase>) =>
    request.post<TestCase>('/content/testcase', data),

  update: (id: string, data: Partial<TestCase>) =>
    request.put<TestCase>(`/content/testcase/${id}`, data),

  delete: (id: string) =>
    request.delete<void>(`/content/testcase/${id}`),

  batchImport: (problemId: string, data: any[]) =>
    request.post<void>(`/content/testcase/problem/${problemId}/batch`, data),

  batchExport: (problemId: string) =>
    request.get<Blob>(`/content/testcase/problem/${problemId}/export`, { responseType: 'blob' })
}

export const judgeConfigApi = {
  get: () =>
    request.get<any>('/content/judge-config'),

  update: (data: any) =>
    request.put<void>('/content/judge-config', data)
}

export const aiPromptApi = {
  getList: (params?: any) =>
    request.get<{ list: AIPrompt[]; total: number }>('/content/ai-prompt', { params }),

  create: (data: Partial<AIPrompt>) =>
    request.post<AIPrompt>('/content/ai-prompt', data),

  update: (id: string, data: Partial<AIPrompt>) =>
    request.put<AIPrompt>(`/content/ai-prompt/${id}`, data),

  delete: (id: string) =>
    request.delete<void>(`/content/ai-prompt/${id}`)
}

export const aiConfigApi = {
  get: () =>
    request.get<any>('/content/ai-config'),

  update: (data: any) =>
    request.put<void>('/content/ai-config', data),

  getDialogues: (params?: any) =>
    request.get<{ list: any[]; total: number }>('/content/ai-dialogue', { params })
}

export const interviewProblemApi = {
  getList: (params?: any) =>
    request.get<{ list: InterviewProblem[]; total: number }>('/interview/admin/problems', { params }),

  getById: (id: string) =>
    request.get<InterviewProblem>(`/interview/admin/problems/${id}`),

  create: (data: Partial<InterviewProblem>) =>
    request.post<InterviewProblem>('/interview/admin/problems', data),

  update: (id: string, data: Partial<InterviewProblem>) =>
    request.put<InterviewProblem>(`/interview/admin/problems/${id}`, data),

  delete: (id: string) =>
    request.delete<void>(`/interview/admin/problems/${id}`),

  changeStatus: (id: string, status: string) =>
    request.put<void>(`/interview/admin/problems/${id}/status`, { status }),

  batchImport: (formData: FormData) =>
    request.post<any>('/interview/admin/problems/batch-import', formData),

  importJson: (formData: FormData) =>
    request.post<any>('/interview/admin/problems/import-json', formData),

  exportJson: () =>
    request.get<Blob>('/interview/admin/problems/export-json', { responseType: 'blob' }),

  aiGenerate: (data: any) =>
    request.post<any>('/interview/admin/ai/generate', data),

  batchSave: (data: any) =>
    request.post<any>('/interview/admin/problems/batch-save', data),

  getStats: () =>
    request.get<any>('/interview/admin/problems/stats')
}
