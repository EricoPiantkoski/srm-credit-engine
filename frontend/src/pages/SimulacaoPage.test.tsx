import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import SimulacaoPage from './SimulacaoPage'

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <SimulacaoPage />
    </QueryClientProvider>,
  )
}

describe('SimulacaoPage', () => {
  it('runs a simulation and shows the result', async () => {
    renderPage()

    await userEvent.selectOptions(screen.getByLabelText('Tipo'), 'DUPLICATA_MERCANTIL')
    expect(screen.getByText('Spread: 1,5% a.m.')).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Valor de face'), '1000')
    await userEvent.selectOptions(screen.getByLabelText('Moeda'), 'BRL')
    expect(screen.getByText('Real brasileiro')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Vencimento'), { target: { value: '2026-09-01' } })
    await userEvent.selectOptions(screen.getByLabelText('Moeda de pagamento'), 'BRL')
    expect(screen.getAllByText('Real brasileiro')).toHaveLength(2)
    await userEvent.click(screen.getByRole('button', { name: 'Simular' }))

    expect(await screen.findByTestId('simulacao-resultado')).toBeInTheDocument()
    expect(screen.getByText('Valor líquido')).toBeInTheDocument()
    expect(screen.getByText('1,5%')).toBeInTheDocument()
  })

  it('validates required fields', async () => {
    renderPage()

    await userEvent.click(screen.getByRole('button', { name: 'Simular' }))

    expect(await screen.findByText('Selecione o tipo')).toBeInTheDocument()
    expect(screen.getByText('Selecione a moeda')).toBeInTheDocument()
  })
})
