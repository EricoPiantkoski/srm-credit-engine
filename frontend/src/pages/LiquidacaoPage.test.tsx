import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import LiquidacaoPage from './LiquidacaoPage'

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <LiquidacaoPage />
    </QueryClientProvider>,
  )
}

describe('LiquidacaoPage', () => {
  it('generates a new key when reloading the liquidation key', async () => {
    renderPage()

    const chave = screen.getByLabelText('Chave') as HTMLInputElement
    const chaveInicial = chave.value
    await userEvent.click(screen.getByRole('button', { name: 'Recarregar chave' }))

    expect(chave.value).not.toBe(chaveInicial)
    expect(chave.value).toMatch(/^[0-9a-f-]{36}$/)
    expect(screen.getByRole('button', { name: 'Recarregar chave' }).querySelector('.reload-icon--spinning')).toBeInTheDocument()
  })

  it('creates a liquidation with a selected receivable', async () => {
    renderPage()

    await userEvent.selectOptions(screen.getByLabelText('Moeda de pagamento'), 'BRL')
    const recebivel = await screen.findByLabelText(/1 — REC-001/)
    await userEvent.click(recebivel)
    await userEvent.click(screen.getByRole('button', { name: 'Liquidar' }))

    expect(await screen.findByTestId('liquidacao-resultado')).toHaveTextContent('LIQUIDADA')
    expect(screen.getByText('Liquidação #1')).toBeInTheDocument()
    expect(screen.queryByLabelText(/1 — REC-001/)).not.toBeInTheDocument()
  })

  it('consults a liquidation by id', async () => {
    renderPage()

    await userEvent.type(screen.getByLabelText('ID da liquidação'), '1')
    await userEvent.click(screen.getByRole('button', { name: 'Consultar' }))

    expect(await screen.findByTestId('liquidacao-resultado')).toHaveTextContent('Liquidação #1')
  })

  it('consults all liquidations', async () => {
    renderPage()

    await userEvent.click(screen.getByRole('button', { name: 'Consultar todas as liquidações' }))

    expect(await screen.findByTestId('liquidacoes-lista')).toHaveTextContent('Liquidação #1')
  })

})
