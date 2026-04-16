import axios from 'axios'

const normalizeBaseUrl = (raw?: string): string => {
  const value = (raw || '/api').trim()
  if (value === '/api') return value
  if (value.endsWith('/api')) return value
  return value.replace(/\/+$/, '') + '/api'
}

export const http = axios.create({
  baseURL: normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL),
  timeout: 60000
})
