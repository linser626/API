import { get, post } from './request'

export const getReferralInfo = () => get('/api/referral/info')
export const getReferralRecords = (params) => get('/api/referral/records', params)
export const applyReferralCode = (referralCode) => post('/api/referral/apply?referralCode=' + encodeURIComponent(referralCode))
