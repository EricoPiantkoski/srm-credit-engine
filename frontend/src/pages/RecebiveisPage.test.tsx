import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import RecebiveisPage from './RecebiveisPage'

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <RecebiveisPage />
    </QueryClientProvider>,
  )
}

describe('RecebiveisPage', () => {
  it('lists receivables', async () => {
    renderPage()

    expect(await screen.findByText('REC-001')).toBeInTheDocument()
    expect(screen.getByText('Fornecedor A')).toBeInTheDocument()
  })

  it('consults one or all receivable details', async () => {
    renderPage()

    await screen.findByText('REC-001')
    await userEvent.click(screen.getByRole('button', { name: 'Ver detalhes do recebível REC-001' }))
    expect(screen.getByRole('region', { name: 'Detalhes do recebível' })).toHaveTextContent('Fornecedor A')

    await userEvent.click(screen.getByRole('button', { name: 'Ver detalhes do recebível REC-001' }))
    expect(screen.queryByRole('region', { name: 'Detalhes do recebível' })).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Consultar todos os recebíveis disponíveis' }))
    expect(screen.getByRole('region', { name: 'Detalhes do recebível' })).toBeInTheDocument()
  })

  it('creates a receivable and shows the confirmation', async () => {
    renderPage()

    await userEvent.type(screen.getByLabelText('Referência externa'), 'REC-100')
    await userEvent.selectOptions(screen.getByLabelText('Tipo'), 'CHEQUE_PRE_DATADO')
    expect(screen.getByText('Spread: 2,5% a.m.')).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Valor de face'), '500')
    await userEvent.selectOptions(screen.getByLabelText('Moeda'), 'USD')
    expect(screen.getByText('Dólar americano')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Vencimento'), { target: { value: '2026-09-01' } })
    await userEvent.type(screen.getByLabelText('Cedente'), 'Fornecedor C')
    await userEvent.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByText('Recebível REC-100 criado.')).toBeInTheDocument()
  })

  it('validates required fields', async () => {
    renderPage()

    await userEvent.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByText('Referência é obrigatória')).toBeInTheDocument()
  })
})
