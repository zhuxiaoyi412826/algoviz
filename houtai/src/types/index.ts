export interface User {
  id: string
  username: string
  nickname: string
  avatar?: string
  email?: string
  phone?: string
  role: 'super_admin' | 'content_admin' | 'operation_admin'
  status: 'active' | 'disabled'
  lastLoginTime?: string
  lastLoginIp?: string
  createTime: string
  updateTime: string
}

export interface LoginLog {
  id: string
  userId: string
  username: string
  ip: string
  device: string
  location?: string
  loginTime: string
  status: 'success' | 'failed'
  failReason?: string
}

export interface OperationLog {
  id: string
  userId: string
  username: string
  module: string
  action: string
  detail: string
  ip: string
  createTime: string
}

export interface SystemConfig {
  key: string
  value: string | number | boolean
  type: 'string' | 'number' | 'boolean' | 'json'
  label: string
  description?: string
  group: 'basic' | 'security' | 'api' | 'theme'
}

export interface DataStructure {
  id: string
  name: string
  type: 'array' | 'linked_list' | 'tree' | 'graph' | 'stack' | 'queue' | 'hash'
  description: string
  status: 'enabled' | 'disabled'
  createTime: string
  updateTime: string
}

export interface Algorithm {
  id: string
  name: string
  category: 'sort' | 'search' | 'graph' | 'dynamic' | 'tree' | 'other'
  description: string
  timeComplexity: string
  spaceComplexity: string
  pseudocode?: string
  status: 'enabled' | 'disabled'
  createTime: string
  updateTime: string
}

export interface OJProblem {
  id: string
  problemNo: string
  title: string
  difficulty: 'easy' | 'medium' | 'hard'
  tags: string[]
  description: string
  template: Record<string, string>
  status: 'online' | 'offline'
  submissionCount: number
  acRate: number
  createTime: string
  updateTime: string
}

export interface TestCase {
  id: string
  problemId: string
  input: string
  output: string
  score: number
  isSample: boolean
}

export interface AIPrompt {
  id: string
  title: string
  category: string
  content: string
  usageCount: number
  status: 'enabled' | 'disabled'
  createTime: string
}

export interface AppUser {
  id: string
  openid: string
  nickname: string
  avatar: string
  bindTime: string
  status: 'active' | 'disabled'
  dsVisits: number
  algoVisits: number
  ojVisits: number
  aiDialogues: number
  lastVisitTime?: string
}

export interface Statistics {
  date: string
  dau: number
  wau: number
  mau: number
  dsVisits: number
  algoVisits: number
  ojSubmissions: number
  ojACRate: number
  aiDialogues: number
}

export interface OJSubmission {
  id: string
  userId: string
  problemId: string
  language: string
  code: string
  status: 'pending' | 'judging' | 'ac' | 'wa' | 'ce' | 're' | 'tle' | 'mle'
  score: number
  timeUsage?: number
  memoryUsage?: number
  createTime: string
}

export interface ServiceStatus {
  name: string
  status: 'running' | 'stopped' | 'error'
  cpu: number
  memory: number
  disk: number
  uptime: string
  lastCheck: string
}

export interface Announcement {
  id: string
  title: string
  content: string
  type: 'update' | 'notice' | 'activity'
  isTop: boolean
  publishTime?: string
  status: 'published' | 'draft' | 'scheduled'
  createTime: string
}

export interface Feedback {
  id: string
  userId: string
  userNickname: string
  type: 'visualization' | 'oj' | 'ai' | 'other'
  content: string
  images?: string[]
  status: 'pending' | 'processing' | 'resolved' | 'rejected'
  reply?: string
  replyTime?: string
  createTime: string
}

export interface MenuItem {
  path: string
  name: string
  icon?: string
  children?: MenuItem[]
  meta?: {
    title: string
    icon?: string
    roles?: string[]
    hidden?: boolean
  }
}
