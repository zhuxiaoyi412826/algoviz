import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { User } from '@/types'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<User | null>(null)
  const roles = ref<string[]>([])

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUserInfo = (info: User) => {
    userInfo.value = info
    roles.value = [info.role]
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    roles.value = []
    localStorage.removeItem('token')
  }

  const hasRole = (role: string) => {
    return roles.value.includes(role)
  }

  return {
    token,
    userInfo,
    roles,
    setToken,
    setUserInfo,
    logout,
    hasRole
  }
})
