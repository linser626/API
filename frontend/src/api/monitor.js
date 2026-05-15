import { get, put } from './request'

export const getUsageStats = (params) => get('/api/monitor/usage', params)
export const getModelUsage = (params) => get('/api/monitor/usage/models', params)
export const getDailyUsage = (params) => get('/api/monitor/usage/daily', params)

export function getNotifications() {
  return get('/api/notifications')
}

export function markNotificationRead(id) {
  return put(`/api/notifications/${id}/read`)
}

export function markAllNotificationsRead() {
  return put('/api/notifications/read-all')
}
