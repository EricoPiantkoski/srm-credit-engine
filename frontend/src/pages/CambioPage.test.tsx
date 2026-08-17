import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import { server } from '../test/server'
import CambioPage from './CambioPage'

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <CambioPage />
    </QueryClientProvider>,
  )
}

describe('CambioPage', () => {
  it('renders the operation sections', () => {
    renderPage()

    expect(screen.getByRole('heading', { name: 'Consultar taxa vigente' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Inserir cotação manualmente' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Buscar Cotação Moeda' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'BRL como moeda de origem' })).toHaveTextContent('BRL | Real')
    expect(screen.getByRole('button', { name: 'USD como moeda de destino' })).toHaveTextContent('USD | Dólar Americano')
    expect(screen.getByRole('heading', { name: 'Converter' })).toBeInTheDocument()
  })

  it('consults the current exchange rate and shows the result', async () => {
    renderPage()

    await userEvent.click(screen.getByRole('button', { name: 'BRL como moeda de origem para consulta' }))
    await userEvent.click(screen.getByRole('button', { name: 'USD como moeda de destino para consulta' }))
    await userEvent.click(screen.getByRole('button', { name: 'Consultar cotação' }))

    expect(await screen.findByTestId('taxa-resultado')).toHaveTextContent('0,18')
  })

  it('shows a friendly message when no rate is found (404)', async () => {
    server.use(
      http.get('*/api/taxas-cambio/vigente', () => {
        return HttpResponse.json({ message: 'not found' }, { status: 404 })
      }),
    )

    renderPage()

    await userEvent.click(screen.getByRole('button', { name: 'BRL como moeda de origem para consulta' }))
    await userEvent.click(screen.getByRole('button', { name: 'USD como moeda de destino para consulta' }))
    await userEvent.click(screen.getByRole('button', { name: 'Consultar cotação' }))

    expect(await screen.findByText('Nenhum registro encontrado.')).toBeInTheDocument()
  })

  it('uses currency buttons and a decimal example for manual quotes', async () => {
    renderPage()

    await userEvent.click(screen.getByRole('button', { name: 'BRL como moeda de origem para inserção' }))
    await userEvent.click(screen.getByRole('button', { name: 'USD como moeda de destino para inserção' }))

    expect(screen.getByLabelText('Cotação')).toHaveAttribute('placeholder', '5.25')
    expect(screen.getByText('Use ponto (.) como separador decimal.')).toBeInTheDocument()
  })

  it('inserts an integration quote with the manual vigency', async () => {
    renderPage()

    await userEvent.click(screen.getByRole('button', { name: 'BRL como moeda de origem para inserção' }))
    await userEvent.click(screen.getByRole('button', { name: 'USD como moeda de destino para inserção' }))
    await userEvent.click(screen.getByRole('button', { name: 'Inserir cotação integração' }))

    expect(await screen.findByTestId('taxa-resultado')).toHaveTextContent('0,18')
  })

  it('selects different origin and destination currencies before searching', async () => {
    renderPage()

    const origemBrl = screen.getByRole('button', { name: 'BRL como moeda de origem' })
    const destinoBrl = screen.getByRole('button', { name: 'BRL como moeda de destino' })
    const origemUsd = screen.getByRole('button', { name: 'USD como moeda de origem' })
    const destinoUsd = screen.getByRole('button', { name: 'USD como moeda de destino' })

    await userEvent.click(origemBrl)
    expect(destinoBrl).not.toBeDisabled()
    expect(destinoBrl).toHaveClass('currency-option--faded')
    expect(origemBrl).toHaveAttribute('aria-pressed', 'true')

    await userEvent.click(destinoUsd)
    expect(origemUsd).not.toBeDisabled()
    expect(origemUsd).toHaveClass('currency-option--faded')
    expect(destinoUsd).toHaveAttribute('aria-pressed', 'true')
    expect(origemBrl).toHaveAttribute('aria-pressed', 'true')

    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))
    expect(await screen.findByTestId('taxa-resultado')).toHaveTextContent('0,18')
  })

  it('replaces the previous selection when the same currency is clicked in the other column', async () => {
    renderPage()

    const origemBrl = screen.getByRole('button', { name: 'BRL como moeda de origem' })
    const destinoBrl = screen.getByRole('button', { name: 'BRL como moeda de destino' })

    await userEvent.click(origemBrl)
    await userEvent.click(destinoBrl)

    expect(origemBrl).toHaveAttribute('aria-pressed', 'false')
    expect(origemBrl).toHaveClass('currency-option--faded')
    expect(destinoBrl).toHaveAttribute('aria-pressed', 'true')
  })

  it('converts a value and shows the result', async () => {
    renderPage()

    await userEvent.type(screen.getByLabelText('Valor'), '100')
    expect(screen.queryByTestId('taxa-resultado')).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'BRL como moeda a converter' }))
    await userEvent.click(screen.getByRole('button', { name: 'USD como moeda de destino para conversão' }))
    expect(await screen.findByTestId('taxa-resultado')).toHaveTextContent('BRL/USD')
    await userEvent.click(screen.getByRole('button', { name: 'Converter' }))

    expect(await screen.findByTestId('conversao-resultado')).toHaveTextContent('R$ 18,00')
    expect(screen.getByLabelText('Precisão decimal')).toHaveAttribute('max', '18')
  })
})
