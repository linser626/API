import { get } from './request'

export const getUsageStats = (params) => get('/api/monitor/usage', params)
export const getModelUsage = (params) => get('/api/monitor/usage/models', params)
export const getDailyUsage = (params) => get('/api/monitor/usage/daily', params)
