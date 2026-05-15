import { get, post } from './request'

export const getOverview = () => get('/api/billing/overview')
export const getTransactions = (params) => get('/api/billing/transactions', params)
export const recharge = (data) => post('/api/billing/recharge', data)
