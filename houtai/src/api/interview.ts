// 面试题后台管理接口 —— 适配文档：/api/interview/admin/*
// 返回结构：{success, code, message, data}；通过 request.ts 解包后直接拿 data
// 注意：vite 代理会在浏览器请求前自动补上 /api 前缀，因此这里写 /interview/admin/... 即可
import request from './request'

export interface AdminPagePayload {
  keyword?: string
  tag?: string
  difficulty?: string
  category?: string
  status?: string   // ACTIVE/INACTIVE（前端页面用 online/offline，会在 Vue 里映射）
  isFrequent?: 1 | 0
  sortBy?: string
  order?: 'asc' | 'desc'
  page: number
  pageSize: number
}

export interface BackendProblem {
  id: number
  problemNo: string
  title: string
  difficulty: 'easy' | 'medium' | 'hard'
  tags: string | string[]
  category: string
  description: string
  inputFormat?: string
  outputFormat?: string
  solution: string
  status: 'ACTIVE' | 'INACTIVE'
  isFrequent: 0 | 1
  isDeleted?: 0 | 1
  viewCount: number
  likeCount?: number
  dislikeCount?: number
  createdAt?: string
  updatedAt?: string
  difficultyLabel?: string
  tagList?: string[]
}

export interface BackendPageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export interface ProblemSavePayload {
  problemNo?: string
  title: string
  difficulty: 'easy' | 'medium' | 'hard'
  category?: string
  tags?: string | string[]
  description?: string
  inputFormat?: string
  outputFormat?: string
  solution?: string
  status?: string
  isFrequent?: 0 | 1 | boolean
}

export interface BatchImportResult {
  total: number
  totalCount?: number
  successNum: number
  successCount?: number
  failNum: number
  failCount?: number
  conflictNum?: number
  conflictCount?: number
  failList?: string[]
  details?: { row: number; problemNo?: string; title?: string; reason: string }[]
  fileName?: string
}

export interface AdminStats {
  totalNum: number
  activeNum: number
  inactiveNum: number
  frequentNum: number
  easyNum: number
  mediumNum: number
  hardNum: number
}

const base = '/interview/admin'

// B1
export function listProblems(params: AdminPagePayload) {
  return request.get<any, BackendPageResult<BackendProblem>>(`${base}/problems`, { params })
}

// B2
export function getProblem(id: number) {
  return request.get<any, BackendProblem>(`${base}/problems/${id}`)
}

// B3
export function createProblem(payload: ProblemSavePayload) {
  return request.post<any, BackendProblem>(`${base}/problems`, payload)
}

// B4
export function updateProblem(id: number, payload: ProblemSavePayload) {
  return request.put<any, void>(`${base}/problems/${id}`, payload)
}

// B5 文档：PUT 切换状态
export function updateProblemStatus(id: number, status: 'ACTIVE' | 'INACTIVE' | string) {
  return request.put<any, void>(`${base}/problems/${id}/status`, { status })
}

// B6 逻辑删除
export function deleteProblem(id: number) {
  return request.delete<any, void>(`${base}/problems/${id}`)
}

// B7 批量逻辑删除
export function batchDeleteProblems(ids: number[]) {
  return request.delete<any, void>(`${base}/problems/batch`, { data: { ids } })
}

// B8 物理删除（文档：/problems/real/{id}）
export function permanentDelete(id: number) {
  return request.delete<any, void>(`${base}/problems/real/${id}`)
}

// B9 批量物理删除（文档：/problems/real/batch）
export function batchPermanentDelete(ids: number[]) {
  return request.delete<any, void>(`${base}/problems/real/batch`, { data: { ids } })
}

// B10 批量导入
export function batchImportProblems(problems: ProblemSavePayload[], overwriteOnConflict = false) {
  return request.post<any, BatchImportResult>(
    `${base}/problems/batch-import?overwriteOnConflict=${overwriteOnConflict}`,
    { problems }
  )
}

// B11 上传 JSON 文件导入
export function importJsonFile(file: File, overwriteOnConflict = false) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post<any, BatchImportResult>(
    `${base}/problems/import-json?overwriteOnConflict=${overwriteOnConflict}`,
    fd,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  )
}

// B12 JSON 导出 URL（window.open 下载）
export function exportProblemsUrl(difficulty?: string, category?: string) {
  const p = new URLSearchParams()
  if (difficulty) p.set('difficulty', difficulty)
  if (category) p.set('category', category)
  const q = p.toString()
  return `/api${base}/problems/export-json${q ? `?${q}` : ''}`
}

// B13 AI 生成
export function aiGenerateProblems(params: { category?: string; difficulty?: string; num: number }) {
  return request.post<any, ProblemSavePayload[]>(`${base}/ai/generate`, params)
}

// B14 保存 AI 生成（文档：/problems/batch-save）
export function aiBatchSave(problems: ProblemSavePayload[]) {
  return request.post<any, BatchImportResult>(`${base}/problems/batch-save`, { problems })
}

// B15 统计
export function adminStats() {
  return request.get<any, AdminStats>(`${base}/stats`)
}

/* ========== 前后台字段值互转 ========== */

/** 前端 status ('online' | 'offline') -> 后端 ('ACTIVE' | 'INACTIVE') */
export function toBackendStatus(st: string): 'ACTIVE' | 'INACTIVE' {
  return st === 'online' ? 'ACTIVE' : 'INACTIVE'
}

/** 后端 status -> 前端 */
export function toFrontStatus(st: string): 'online' | 'offline' {
  return st === 'ACTIVE' ? 'online' : 'offline'
}

/** 后端 isFrequent (0/1) -> 前端 boolean */
export function toFrontFreq(n: number | boolean | undefined | null): boolean {
  if (typeof n === 'boolean') return n
  return n === 1
}

/** 前端 boolean -> 后端 1/0 */
export function toBackendFreq(b: boolean | number | undefined | null): 1 | 0 {
  if (typeof b === 'number') return b === 0 ? 0 : 1
  return b ? 1 : 0
}

/** 后端 tags (string) -> 前端 string[] */
export function toFrontTags(tags: string | string[] | undefined | null): string[] {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  return tags.split(/[,，;；|]/).map(s => s.trim()).filter(Boolean)
}

/** BackendProblem -> 前端 InterviewProblem */
export function convertToFront(p: BackendProblem): any {
  return {
    id: p.id,
    problemNo: p.problemNo,
    title: p.title,
    difficulty: p.difficulty,
    tags: p.tagList && p.tagList.length ? p.tagList : toFrontTags(p.tags),
    category: p.category || '',
    description: p.description || '',
    inputFormat: p.inputFormat || '',
    outputFormat: p.outputFormat || '',
    solution: p.solution || '',
    isFrequent: toFrontFreq(p.isFrequent),
    status: toFrontStatus(p.status),
    viewCount: p.viewCount || 0,
    createTime: p.createdAt ? new Date(p.createdAt).toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-') : ''
  }
}

/** 前端 save form -> Backend save payload */
export function convertToBackend(form: any): ProblemSavePayload {
  return {
    problemNo: form.problemNo || undefined,
    title: form.title,
    difficulty: form.difficulty,
    category: form.category || '',
    tags: form.tags || [],
    description: form.description || '',
    inputFormat: form.inputFormat || '',
    outputFormat: form.outputFormat || '',
    solution: form.solution || '',
    status: toBackendStatus(form.status || 'offline'),
    isFrequent: toBackendFreq(form.isFrequent)
  }
}
