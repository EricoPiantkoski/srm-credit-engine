import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { ErrorNotice } from '../components/ErrorNotice'
import { StatusBadge } from '../components/StatusBadge'
import {
  CODIGOS_MOEDA_PAGAMENTO,
  fetchExtrato,
  STATUS_LIQUIDACAO,
  type ExtratoFiltros,
} from '../features/extrato/api'
import { formatInstant, formatMoney } from '../lib/format'

const extratoSchema = z.object({
  dataInicial: z.string(),
  dataFinal: z.string(),
  status: z.string(),
  cedente: z.string(),
  codigoMoedaPagamento: z.string(),
})

type ExtratoForm = z.infer<typeof extratoSchema>

function toFiltros(values: ExtratoForm, limit: number): ExtratoFiltros {
  const filtros: ExtratoFiltros = { limit }
  if (values.dataInicial) filtros.dataInicial = values.dataInicial
  if (values.dataFinal) filtros.dataFinal = values.dataFinal
  if (values.status) filtros.status = values.status
  if (values.cedente) filtros.cedente = values.cedente
  if (values.codigoMoedaPagamento) filtros.codigoMoedaPagamento = values.codigoMoedaPagamento
  return filtros
}

export default function ExtratoPage() {
  const [filtros, setFiltros] = useState<ExtratoFiltros>({ limit: 20 })
  const [cursorHistory, setCursorHistory] = useState<Array<number | undefined>>([undefined])
  const { register, handleSubmit, formState } = useForm<ExtratoForm>({
    resolver: zodResolver(extratoSchema),
  })

  const { data, isLoading, isFetching, error } = useQuery({
    queryKey: ['extrato', filtros],
    queryFn: () => fetchExtrato(filtros),
  })

  function onSubmit(values: ExtratoForm) {
    setCursorHistory([undefined])
    setFiltros(toFiltros(values, filtros.limit ?? 20))
  }

  function goToNextPage() {
    if (!data || data.length < (filtros.limit ?? 20)) return
    const nextCursor = data[data.length - 1]?.itemId
    if (nextCursor === undefined) return
    setCursorHistory((history) => [...history, nextCursor])
    setFiltros((current) => ({ ...current, lastId: nextCursor }))
  }

  function goToPreviousPage() {
    if (cursorHistory.length <= 1) return
    const previousHistory = cursorHistory.slice(0, -1)
    setCursorHistory(previousHistory)
    setFiltros((current) => ({ ...current, lastId: previousHistory[previousHistory.length - 1] }))
  }

  return (
    <main className="page">
      <header className="page__header">
        <h1 className="page__title">Extrato de Liquidações</h1>
        <p className="page__subtitle">Consulte o detalhamento de liquidações por período, status e cedente</p>
      </header>

      <section className="card">
        <div className="card__header">
          <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M3 4h18 M3 8h18 M3 12h18 M3 16h18 M3 20h18" />
          </svg>
          <h2 className="card__title">Filtros</h2>
        </div>
        <div className="card__body">
          <form className="form" onSubmit={handleSubmit(onSubmit)} noValidate>
            <div className="form__row">
              <div className="form__group">
                <label className="label" htmlFor="extrato-inicial">
                  Data inicial
                </label>
                <input id="extrato-inicial" className="input" type="date" {...register('dataInicial')} />
              </div>
              <div className="form__group">
                <label className="label" htmlFor="extrato-final">
                  Data final
                </label>
                <input id="extrato-final" className="input" type="date" {...register('dataFinal')} />
              </div>
            </div>

            <div className="form__row">
              <div className="form__group">
                <label className="label" htmlFor="extrato-status">
                  Status
                </label>
                <select id="extrato-status" className="select" {...register('status')}>
                  <option value="">Todos</option>
                  {STATUS_LIQUIDACAO.map((status) => (
                    <option key={status} value={status}>
                      {status}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form__group">
                <label className="label" htmlFor="extrato-cedente">
                  Cedente
                </label>
                <input id="extrato-cedente" className="input" placeholder="Fornecedor A" {...register('cedente')} />
              </div>
            </div>

            <div className="form__row">
              <div className="form__group">
                <label className="label" htmlFor="extrato-moeda">
                  Moeda de pagamento
                </label>
                <select id="extrato-moeda" className="select" {...register('codigoMoedaPagamento')}>
                  <option value="">Todas</option>
                  {CODIGOS_MOEDA_PAGAMENTO.map((moeda) => (
                    <option key={moeda} value={moeda}>
                      {moeda}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div>
              <button className="btn btn--primary" type="submit" data-tooltip="Filtrar o extrato" disabled={formState.isSubmitting}>
                Filtrar
              </button>
            </div>
          </form>
        </div>
      </section>

      <section className="card">
        <div className="card__header">
          <h2 className="card__title">Resultado</h2>
          <div className="extrato-pagination" aria-label="Paginação do extrato">
            <button className="btn btn--secondary" type="button" data-tooltip="Consultar página anterior" disabled={cursorHistory.length <= 1 || isFetching} onClick={goToPreviousPage}>
              Anterior
            </button>
            <span>Página {cursorHistory.length}</span>
            <button className="btn btn--secondary" type="button" data-tooltip="Consultar próxima página" disabled={!data || data.length < (filtros.limit ?? 20) || isFetching} onClick={goToNextPage}>
              Próxima
            </button>
            <label className="pagination__size">
              Itens por página
              <select value={filtros.limit ?? 20} onChange={(event) => {
                const limit = Number(event.target.value)
                setCursorHistory([undefined])
                setFiltros((current) => ({ ...current, limit, lastId: undefined }))
              }}>
                <option value={20}>20</option>
                <option value={50}>50</option>
                <option value={100}>100</option>
              </select>
            </label>
          </div>
        </div>
        <div className="card__body">
          {isLoading || isFetching ? (
            <p className="loading" role="status">
              <span className="loading__spinner" aria-hidden="true" />
              Carregando…
            </p>
          ) : data?.length === 0 ? (
            <div className="empty">
              <span className="empty__icon" aria-hidden="true">∅</span>
              <p>Nenhum item encontrado.</p>
            </div>
          ) : (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Liquidação</th>
                    <th>Status</th>
                    <th>Data</th>
                    <th>Recebível</th>
                    <th>Cedente</th>
                    <th>Valor pago</th>
                    <th>Moeda</th>
                    <th>Taxa</th>
                  </tr>
                </thead>
                <tbody>
                  {data?.map((item) => (
                    <tr key={item.itemId}>
                      <td className="mono">{item.liquidacaoId}</td>
                      <td>
                        <StatusBadge status={item.status} />
                      </td>
                      <td>{formatInstant(item.createdAt)}</td>
                      <td className="mono">{item.recebivelId}</td>
                      <td>{item.cedente}</td>
                      <td className="mono">{formatMoney(item.valorPagamento, item.codigoMoedaPagamento)}</td>
                      <td>{item.codigoMoedaPagamento}</td>
                      <td className="mono">{item.taxaAplicada.toLocaleString('pt-BR')}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <ErrorNotice error={error} />
        </div>
      </section>
    </main>
  )
}
