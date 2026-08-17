import { http as mswHttp, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { onUnauthorized } from '../../features/auth/refresh'
import { useSession } from '../../features/auth/session'
import { server } from '../../test/server'
import { http } from './http'

describe('http', () => {
  beforeEach(() => {
    useSession.getState().clear()
  })

  it('sends the access token in the Authorization header', async () => {
    useSession
      .getState()
      .setTokens({ accessToken: 'tok', refreshToken: 'rt', accessTokenExpiresAt: '2026-08-17T12:00:00Z' })

    let received: string | null = null
    server.use(
      mswHttp.get('*/api/things', ({ request }) => {
        received = request.headers.get('Authorization')
        return HttpResponse.json([{ id: 1 }])
      }),
    )

    const result = await http<{ id: number }[]>('/api/things')

    expect(received).toBe('Bearer tok')
    expect(result).toEqual([{ id: 1 }])
  })

  it('refreshes once and retries the request after a 401', async () => {
    useSession
      .getState()
      .setTokens({ accessToken: 'expired', refreshToken: 'rt', accessTokenExpiresAt: '2026-08-17T12:00:00Z' })

    let calls = 0
    server.use(
      mswHttp.get('*/api/things', ({ request }) => {
        calls += 1
        if (request.headers.get('Authorization') === 'Bearer expired') {
          return HttpResponse.json({ message: 'unauthorized' }, { status: 401 })
        }
        return HttpResponse.json([{ ok: true }])
      }),
    )

    const result = await http<{ ok: boolean }[]>('/api/things')

    expect(calls).toBe(2)
    expect(result).toEqual([{ ok: true }])
    expect(useSession.getState().accessToken).toBe('access-token-new')
    expect(useSession.getState().refreshToken).toBe('refresh-token-new')
  })

  it('clears the session and notifies unauthorized when refresh fails', async () => {
    useSession
      .getState()
      .setTokens({ accessToken: 'expired', refreshToken: 'rt', accessTokenExpiresAt: '2026-08-17T12:00:00Z' })

    server.use(
      mswHttp.get('*/api/things', () => HttpResponse.json({ message: 'unauthorized' }, { status: 401 })),
      mswHttp.post('*/api/auth/refresh', () => HttpResponse.json({ message: 'invalid' }, { status: 401 })),
    )

    const unauthorized = vi.fn()
    const unsubscribe = onUnauthorized(unauthorized)

    await expect(http('/api/things')).rejects.toThrow()
    expect(unauthorized).toHaveBeenCalledTimes(1)
    expect(useSession.getState().accessToken).toBeUndefined()
    expect(useSession.getState().refreshToken).toBeUndefined()

    unsubscribe()
  })

  it('does not attempt refresh for auth endpoints', async () => {
    let refreshCalled = false
    server.use(
      mswHttp.post('*/api/auth/login', () => HttpResponse.json({ message: 'invalid' }, { status: 401 })),
      mswHttp.post('*/api/auth/refresh', () => {
        refreshCalled = true
        return HttpResponse.json({})
      }),
    )

    await expect(http('/api/auth/login', { method: 'POST' })).rejects.toThrow()
    expect(refreshCalled).toBe(false)
  })
})