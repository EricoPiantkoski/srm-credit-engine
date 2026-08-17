import { ApiError, http } from '../../lib/api/http'
import { useSession } from './session'
import type { AuthResponse } from './types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

async function authFetch<T>(path: string, body: object): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    throw new ApiError(response.status, `Auth request to ${path} failed with ${response.status}`)
  }
  return (await response.json()) as T
}

export async function loginRequest(username: string, password: string): Promise<AuthResponse> {
  const tokens = await authFetch<AuthResponse>('/api/auth/login', { username, password })
  useSession.getState().setTokens(tokens)
  return tokens
}

export async function logoutRequest(refreshToken: string): Promise<void> {
  try {
    await http<void>(
      '/api/auth/logout',
      { method: 'POST', body: JSON.stringify({ refreshToken }) },
      { skipRefresh: true },
    )
  } catch {
    // sessão é limpa localmente mesmo se o servidor rejeitar o logout
  } finally {
    useSession.getState().clear()
  }
}