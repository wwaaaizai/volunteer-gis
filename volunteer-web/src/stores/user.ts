import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/api'

export interface UserInfo {
  userId: string
  studentId: string
  role: string
  name?: string
  phone?: string
  grade?: string
  college?: string
  organization?: string
  totalHours?: string
}

export const useUserStore = defineStore('user', () => {
  const user = ref<UserInfo | null>(null)
  const token = ref<string>(localStorage.getItem('token') || '')

  // 登录
  async function login(studentId: string, password: string, rememberMe: boolean = false) {
    const data = await request.post('/auth/login', { studentId, password }) as any
    token.value = data.token
    localStorage.setItem('token', data.token)
    // 记住我：缓存/清除学号
    if (rememberMe) {
      localStorage.setItem('rememberedStudentId', studentId)
      localStorage.setItem('rememberMe', 'true')
    } else {
      localStorage.removeItem('rememberedStudentId')
      localStorage.setItem('rememberMe', 'false')
    }
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

  // 退出登录（保留记住的学号，方便下次自动填充）
  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
  }

  /** 获取记住的学号（供登录页自动填充） */
  function getRememberedStudentId(): string {
    if (localStorage.getItem('rememberMe') === 'true') {
      return localStorage.getItem('rememberedStudentId') || ''
    }
    return ''
  }

  return { user, token, login, fetchUser, logout, getRememberedStudentId }
})
