import { get, post } from './request'

export const pay = (data) => post('/api/payment/pay', data)
export const getOrders = (params) => get('/api/payment/orders', params)
export const getOrder = (orderNo) => get(`/api/payment/orders/${orderNo}`)
