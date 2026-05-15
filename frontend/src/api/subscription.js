import { get, post, put } from './request'

export const listPlans = () => get('/api/subscription/plans')
export const getCurrentSubscription = () => get('/api/subscription/current')
export const subscribe = (data) => post('/api/subscription/subscribe', data)
export const cancelSubscription = () => put('/api/subscription/cancel')
export const getQuota = () => get('/api/subscription/quota')
