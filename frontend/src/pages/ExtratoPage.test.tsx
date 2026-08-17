import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import ExtratoPage from './ExtratoPage'

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <ExtratoPage />
    </QueryClientProvider>,
  )
}

describe('ExtratoPage', () => {
  it('renders the statement rows', async () => {
    renderPage()

    expect(await screen.findByText('Fornecedor A')).toBeInTheDocument()
    expect(screen.getAllByText('LIQUIDADA').length).toBeGreaterThanOrEqual(2)
  })

  it('applies filters', async () => {
    renderPage()

    fireEvent.change(screen.getByLabelText('Data inicial'), { target: { value: '2026-08-01' } })
    await userEvent.click(screen.getByRole('button', { name: 'Filtrar' }))

    expect(await screen.findByText('Fornecedor A')).toBeInTheDocument()
  })
})