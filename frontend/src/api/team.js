import { get, post, put, del } from './request'

export const createTeam = (data) => post('/api/teams', data)
export const getMyTeams = () => get('/api/teams')
export const getTeamDetail = (id) => get(`/api/teams/${id}`)
export const updateTeam = (id, data) => put(`/api/teams/${id}`, data)
export const deleteTeam = (id) => del(`/api/teams/${id}`)

export const getTeamMembers = (teamId) => get(`/api/teams/${teamId}/members`)
export const inviteMember = (teamId, data) => post(`/api/teams/${teamId}/members/invite`, data)
export const removeMember = (teamId, memberId) => del(`/api/teams/${teamId}/members/${memberId}`)
export const updateMemberRole = (teamId, memberId, role) => put(`/api/teams/${teamId}/members/${memberId}/role`, { role })
export const leaveTeam = (teamId) => post(`/api/teams/${teamId}/leave`)

export const createTeamApiKey = (teamId, data) => post(`/api/teams/${teamId}/apikeys`, data)
export const getTeamApiKeys = (teamId) => get(`/api/teams/${teamId}/apikeys`)
export const revokeTeamApiKey = (teamId, keyId) => del(`/api/teams/${teamId}/apikeys/${keyId}`)

export const rechargeTeamBalance = (teamId, amount) => post(`/api/teams/${teamId}/recharge`, { amount })
