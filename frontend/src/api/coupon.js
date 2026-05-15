import { get, post } from './request'

export const redeemCoupon = (data) => post('/api/coupon/redeem', data)
export const getMyCoupons = (params) => get('/api/coupon/my', params)
