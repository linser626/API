import { get, post, put } from './request'

export const login = (data) => post('/api/auth/login', data)
export const register = (data) => post('/api/auth/register', data)
export const refreshToken = (data) => post('/api/auth/refresh', data)
export const getProfile = () => get('/api/user/profile')
export const updateProfile = (data) => put('/api/user/profile', data)
export const changePassword = (data) => put('/api/user/password', data)
