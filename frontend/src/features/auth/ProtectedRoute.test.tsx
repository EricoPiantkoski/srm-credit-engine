import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

import { ProtectedRoute } from './ProtectedRoute'
import { useSession } from './session'

function renderRoute() {
  return render(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <p>conteúdo protegido</p>
            </ProtectedRoute>
          }
        />
        <Route path="/login" element={<p>tela de login</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    useSession.getState().clear()
  })

  it('renders children when authenticated', () => {
    useSession
      .getState()
      .setTokens({ accessToken: 'tok', refreshToken: 'rt', accessTokenExpiresAt: '2026-08-17T12:00:00Z' })

    renderRoute()

    expect(screen.getByText('conteúdo protegido')).toBeInTheDocument()
  })

  it('redirects to login when not authenticated', () => {
    renderRoute()

    expect(screen.getByText('tela de login')).toBeInTheDocument()
    expect(screen.queryByText('conteúdo protegido')).not.toBeInTheDocument()
  })
})