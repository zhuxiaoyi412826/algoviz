import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { User } from '@/types'

function loadFromStorage<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return fallback
    return JSON.parse(raw) as T
  } catch {
    return fallback
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<User | null>(loadFromStorage<User | null>('userInfo', null))
  const roles = ref<string[]>(loadFromStorage<string[]>('roles', []))
  const permissions = ref<string[]>(loadFromStorage<string[]>('permissions', []))

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUserInfo = (info: User, userRoles: string[] = [], userPermissions: string[] = []) => {
    userInfo.value = info
    roles.value = userRoles.length > 0 ? userRoles : [info.role]
    permissions.value = userPermissions
  }

  const setRoles = (userRoles: string[]) => {
    roles.value = userRoles
  }

  const setPermissions = (userPermissions: string[]) => {
    permissions.value = userPermissions
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    roles.value = []
    permissions.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  const hasRole = (role: string) => {
    return roles.value.includes(role)
  }

  const hasAnyRole = (roleList: string[]) => {
    return roleList.some(role => roles.value.includes(role))
  }

  // 判断是否是超级管理员（拥有 SUPER_ADMIN 角色）
  const isSuperAdmin = () => {
    return roles.value.includes('SUPER_ADMIN') || roles.value.includes('super_admin')
  }

  // 判断是否是一级管理员（拥有 LEVEL1_ADMIN 角色）
  const isLevel1Admin = () => {
    return roles.value.includes('LEVEL1_ADMIN') || roles.value.includes('level1_admin')
  }

  return {
    token,
    userInfo,
    roles,
    permissions,
    setToken,
    setUserInfo,
    setRoles,
    setPermissions,
    logout,
    hasRole,
    hasAnyRole,
    isSuperAdmin,
    isLevel1Admin
  }
})
