import { get, post } from './request'

export function chatPlayground(data) {
  return post('/api/playground/chat', data)
}

export function getPlaygroundModels() {
  return get('/api/playground/models')
}
