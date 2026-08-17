import { useSession } from './session'
import type { AuthResponse } from './types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

let refreshPromise: Promise<boolean> | null = null

const unauthorizedListeners = new Set<() => void>()

export function onUnauthorized(listener: () => void): () => void {
  unauthorizedListeners.add(listener)
  return () => unauthorizedListeners.delete(listener)
}

export function notifyUnauthorized(): void {
  unauthorizedListeners.forEach((listener) => listener())
}

async function requestRefresh(refreshToken: string): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })
  if (!response.ok) {
    throw new Error('Refresh token rejeitado')
  }
  return (await response.json()) as AuthResponse
}

export async function ensureFreshAccessToken(): Promise<boolean> {
  const refreshToken = useSession.getState().refreshToken
  if (!refreshToken) {
    return false
  }
  if (!refreshPromise) {
    refreshPromise = requestRefresh(refreshToken)
      .then((tokens) => {
        useSession.getState().setTokens(tokens)
        return true
      })
      .catch(() => {
        useSession.getState().clear()
        notifyUnauthorized()
        return false
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}