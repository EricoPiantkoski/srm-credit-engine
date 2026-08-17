import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'

import AccessDeniedPage from './AccessDeniedPage'

describe('AccessDeniedPage', () => {
  it('renders access denied message', () => {
    render(
      <MemoryRouter>
        <AccessDeniedPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Acesso negado' })).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent('não tem permissão')
    expect(screen.getByRole('link', { name: 'Voltar ao início' })).toBeInTheDocument()
  })
})