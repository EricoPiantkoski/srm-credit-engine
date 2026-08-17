import * as Sentry from '@sentry/react'
import { ensureFreshAccessToken } from '../../features/auth/refresh'
import { useSession } from '../../features/auth/session'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

interface HttpOptions {
  auth?: boolean
  skipRefresh?: boolean
}

function captureApiError(error: unknown): void {
  if (!(error instanceof ApiError) || [401, 403, 404].includes(error.status)) {
    return
  }

  Sentry.captureException(error, {
    tags: {
      error_type: 'api',
      http_status: String(error.status),
    },
  })
}

function buildHeaders(init: RequestInit | undefined, auth: boolean): HeadersInit {
  const headers = { 'Content-Type': 'application/json', ...init?.headers } as Record<string, string>
  const accessToken = useSession.getState().accessToken
  if (auth && accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`
  }
  return headers
}

async function doFetch<T>(path: string, init: RequestInit | undefined, auth: boolean): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: buildHeaders(init, auth),
  })

  if (!response.ok) {
    throw new ApiError(response.status, `Request to ${path} failed with ${response.status}`)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

export async function http<T>(path: string, init?: RequestInit, options: HttpOptions = {}): Promise<T> {
  const { auth = true, skipRefresh = false } = options
  const shouldRefresh = auth && !skipRefresh && !path.startsWith('/api/auth/')

  try {
    return await doFetch<T>(path, init, auth)
  } catch (error) {
    if (error instanceof ApiError && error.status === 401 && shouldRefresh) {
      const refreshed = await ensureFreshAccessToken()
      if (refreshed) {
        try {
          return await doFetch<T>(path, init, auth)
        } catch (retryError) {
          captureApiError(retryError)
          throw retryError
        }
      }
      captureApiError(error)
      throw error
    }
    captureApiError(error)
    throw error
  }
}
