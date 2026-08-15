import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import HomePage from './HomePage'

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <HomePage />
    </QueryClientProvider>,
  )
}

describe('HomePage', () => {
  it('renders service status UP', async () => {
    renderPage()

    expect(await screen.findByText('Status do serviço: UP')).toBeInTheDocument()
  })
})
