import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export interface SessionTokens {
  accessToken: string
  refreshToken: string
  accessTokenExpiresAt: string
}

interface SessionState extends Partial<SessionTokens> {
  setTokens: (tokens: SessionTokens) => void
  clear: () => void
}

export const useSession = create<SessionState>()(
  persist(
    (set) => ({
      accessToken: undefined,
      refreshToken: undefined,
      accessTokenExpiresAt: undefined,
      setTokens: ({ accessToken, refreshToken, accessTokenExpiresAt }) =>
        set({ accessToken, refreshToken, accessTokenExpiresAt }),
      clear: () => set({ accessToken: undefined, refreshToken: undefined, accessTokenExpiresAt: undefined }),
    }),
    { name: 'srm-auth' },
  ),
)

export function isAuthenticated(state: Pick<SessionState, 'accessToken'>): boolean {
  return Boolean(state.accessToken)
}