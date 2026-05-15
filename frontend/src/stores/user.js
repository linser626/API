import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi, refreshToken as refreshTokenApi, getProfile } from '@/api/auth'
import router from '@/router'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    refreshToken: localStorage.getItem('refreshToken') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}')
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.userInfo.role === 'admin',
    balance: (state) => state.userInfo.balance || 0
  },

  actions: {
    async login(credentials) {
      const res = await loginApi(credentials)
      this.token = res.data.token
      this.refreshToken = res.data.refresh_token
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('refreshToken', res.data.refresh_token)
      await this.getUserInfo()
      return res
    },

    async register(data) {
      const res = await registerApi(data)
      return res
    },

    async getUserInfo() {
      try {
        const res = await getProfile()
        this.userInfo = res.data
        localStorage.setItem('userInfo', JSON.stringify(res.data))
        return res.data
      } catch (error) {
        throw error
      }
    },

    async refresh() {
      try {
        const res = await refreshTokenApi({ refresh_token: this.refreshToken })
        this.token = res.data.token
        this.refreshToken = res.data.refresh_token
        localStorage.setItem('token', res.data.token)
        localStorage.setItem('refreshToken', res.data.refresh_token)
      } catch (error) {
        this.logout()
        throw error
      }
    },

    logout() {
      this.token = ''
      this.refreshToken = ''
      this.userInfo = {}
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userInfo')
      router.push('/login')
    }
  }
})
