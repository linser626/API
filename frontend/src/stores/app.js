import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: localStorage.getItem('sidebarCollapsed') === 'true',
    theme: localStorage.getItem('theme') || 'light',
    locale: localStorage.getItem('locale') || 'zh'
  }),

  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      localStorage.setItem('sidebarCollapsed', this.sidebarCollapsed)
    },

    setTheme(theme) {
      this.theme = theme
      localStorage.setItem('theme', theme)
      document.documentElement.setAttribute('data-theme', theme)
    },

    setLocale(locale) {
      this.locale = locale
      localStorage.setItem('locale', locale)
    }
  }
})
