import { get, post, put, del } from './request'

export const createApiKey = (data) => post('/api/apikeys', data)
export const listApiKeys = () => get('/api/apikeys')
export const updateApiKey = (id, data) => put(`/api/apikeys/${id}`, data)
export const deleteApiKey = (id) => del(`/api/apikeys/${id}`)
