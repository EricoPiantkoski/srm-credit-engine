import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { MemoryRouter } from 'react-router-dom'

import HomePage from './HomePage'

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('HomePage', () => {
  it('renders service status UP', async () => {
    renderPage()

    expect(await screen.findByText('Status do serviço: UP')).toBeInTheDocument()
  })

  it('links to the operation panels', async () => {
    renderPage()

    expect(await screen.findByRole('link', { name: /Câmbio/ })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Extrato/ })).toBeInTheDocument()
  })
})