import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

import { DashboardLayout } from './DashboardLayout'
import { useSession } from './session'

function renderLayout() {
  return render(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route path="/" element={<DashboardLayout><p>home</p></DashboardLayout>} />
        <Route path="/login" element={<p>tela de login</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('DashboardLayout', () => {
  beforeEach(() => {
    useSession.getState().clear()
  })

  it('logs out, clears the session and navigates to login', async () => {
    useSession
      .getState()
      .setTokens({ accessToken: 'tok', refreshToken: 'rt', accessTokenExpiresAt: '2026-08-17T12:00:00Z' })

    renderLayout()

    await userEvent.click(screen.getByRole('button', { name: 'Sair' }))

    expect(await screen.findByText('tela de login')).toBeInTheDocument()
    expect(useSession.getState().accessToken).toBeUndefined()
    expect(useSession.getState().refreshToken).toBeUndefined()
  })
})