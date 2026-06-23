import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/api'

export interface UserInfo {
  userId: string
  studentId: string
  role: string
  name?: string
  phone?: string
  organization?: string
}

export const useUserStore = defineStore('user', () => {
  const user = ref<UserInfo | null>(null)
  const token = ref<string>(localStorage.getItem('token') || '')

  // 登录
  async function login(studentId: string, password: string) {
    const data = await request.post('/auth/login', { studentId, password }) as any
    token.value = data.token
    localStorage.setItem('token', data.token)
    await fetchUser()
  }

  // 获取当前用户信息
  async function fetchUser() {
    if (!token.value) return
    try {
      user.value = await request.get('/auth/me') as any
    } catch {
      logout()
    }
  }

  // 退出登录
  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
  }

  return { user, token, login, fetchUser, logout }
})
