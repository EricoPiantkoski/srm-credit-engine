import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { ErrorNotice } from '../components/ErrorNotice'
import { StatusBadge } from '../components/StatusBadge'
import { listRecebiveis, type Recebivel } from '../features/recebiveis/api'
import {
  createLiquidacao,
  listLiquidacoes,
  obtainLiquidacao,
  type ItemLiquidacao,
  type Liquidacao,
} from '../features/liquidacao/api'
import { CODIGOS_MOEDA_PAGAMENTO } from '../features/extrato/api'
import { formatDate, formatInstant, formatMoney } from '../lib/format'

function gerarUuid(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (char) => {
    const random = (Math.random() * 16) | 0
    return (char === 'x' ? random : (random & 0x3) | 0x8).toString(16)
  })
}

const liquidacaoSchema = z.object({
  chaveIdempotencia: z
    .string()
    .regex(/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/, 'Chave deve ser um UUID'),
  codigoMoedaPagamento: z.enum(CODIGOS_MOEDA_PAGAMENTO, { message: 'Selecione a moeda de pagamento' }),
  recebiveisIds: z
    .array(z.string())
    .min(1, 'Selecione ao menos um recebível')
    .or(z.string().min(1, 'Selecione ao menos um recebível')),
})

type LiquidacaoForm = z.infer<typeof liquidacaoSchema>
const consultaSchema = z.object({ id: z.number().int().positive('Informe um ID válido') })
type ConsultaForm = z.infer<typeof consultaSchema>

function ItensTable({ itens }: { itens: ItemLiquidacao[] }) {
  return (
    <div className="table-wrap">
      <table className="data-table">
        <thead>
          <tr>
            <th>Recebível</th>
            <th>Valor presente</th>
            <th>Valor pago</th>
            <th>Moeda</th>
            <th>Prazo</th>
            <th>Taxa</th>
          </tr>
        </thead>
        <tbody>
          {itens.map((item) => (
            <tr key={item.recebivelId}>
              <td className="mono">{item.recebivelId}</td>
              <td className="mono">{formatMoney(item.valorPresente, item.codigoMoedaPagamento)}</td>
              <td className="mono">{formatMoney(item.valorPagamento, item.codigoMoedaPagamento)}</td>
              <td>{item.codigoMoedaPagamento}</td>
              <td>{item.prazoMeses}</td>
              <td className="mono">{item.taxaAplicada.toLocaleString('pt-BR')}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function LiquidacaoResultado({ liquidacao }: { liquidacao: Liquidacao }) {
  return (
    <>
      <section className="result-card" aria-label="Resultado da liquidação" data-testid="liquidacao-resultado">
        <div className="result-card__header">
          <div>
            <h2 className="result-card__title">Liquidação #{liquidacao.id}</h2>
            <p className="result-card__meta">Criada em {formatInstant(liquidacao.createdAt)}</p>
          </div>
          <StatusBadge status={liquidacao.status} />
        </div>
        <ItensTable itens={liquidacao.itens} />
      </section>
    </>
  )
}

function CriarLiquidacao() {
  const [error, setError] = useState<unknown>(null)
  const [liquidacaoAtual, setLiquidacaoAtual] = useState<Liquidacao | null>(null)
  const [reloadCount, setReloadCount] = useState(0)
  const [liquidatedRecebiveis, setLiquidatedRecebiveis] = useState<Set<number>>(() => new Set())
  const [recebivelDetalhes, setRecebivelDetalhes] = useState<Recebivel | null>(null)
  const { data: recebiveis, isLoading } = useQuery({ queryKey: ['recebiveis'], queryFn: listRecebiveis })
  const { register, handleSubmit, setValue, formState } = useForm<LiquidacaoForm>({
    resolver: zodResolver(liquidacaoSchema),
    defaultValues: { chaveIdempotencia: gerarUuid() },
  })

  const mutation = useMutation({ mutationFn: createLiquidacao })

  async function onSubmit(values: LiquidacaoForm) {
    setError(null)
    try {
      const ids = Array.isArray(values.recebiveisIds) ? values.recebiveisIds : [values.recebiveisIds]
      const result = await mutation.mutateAsync({ ...values, recebiveisIds: ids.map(Number) })
      setLiquidacaoAtual(result)
      setLiquidatedRecebiveis((current) => new Set([...current, ...ids.map(Number)]))
      setRecebivelDetalhes(null)
    } catch (err) {
      setError(err)
    }
  }

  const disponiveis = recebiveis?.filter((recebivel) =>
    recebivel.status === 'DISPONIVEL' && !liquidatedRecebiveis.has(recebivel.id),
  ) ?? []

  return (
    <>
      <section className="card">
        <div className="card__header">
          <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M9 11H7a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-7a2 2 0 0 0-2-2h-2 M12 15v-8 M8 9l4-4 4 4" />
          </svg>
          <h2 className="card__title">Criar liquidação</h2>
        </div>
        <div className="card__body">
          <form className="form" onSubmit={handleSubmit(onSubmit)} noValidate>
            <div className="form__row">
              <div className="form__group">
                <label className="label" htmlFor="chaveIdempotencia">
                  Chave
                </label>
                  <div className="input-with-action">
                    <input id="chaveIdempotencia" className="input mono" {...register('chaveIdempotencia')} />
                    <button
                      className="btn btn--secondary reload-button"
                      type="button"
                      data-tooltip="Gerar uma nova chave UUID"
                      aria-label="Recarregar chave"
                      onClick={() => {
                        setValue('chaveIdempotencia', gerarUuid(), { shouldDirty: true, shouldValidate: true })
                        setReloadCount((count) => count + 1)
                      }}
                    >
                      <svg key={reloadCount} className={`icon${reloadCount ? ' reload-icon--spinning' : ''}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                        <path d="M21 2v6h-6 M3 12a9 9 0 0 1 15-6.7L21 8 M3 22v-6h6 M21 12a9 9 0 0 1-15 6.7L3 16" />
                      </svg>
                    </button>
                  </div>
                {formState.errors.chaveIdempotencia && (
                  <p className="field-error" role="alert">{formState.errors.chaveIdempotencia.message}</p>
                )}
              </div>
              <div className="form__group">
                <label className="label" htmlFor="codigoMoedaPagamento">
                  Moeda de pagamento
                </label>
                <select id="codigoMoedaPagamento" className="select" {...register('codigoMoedaPagamento')}>
                  <option value="">Selecione…</option>
                  {CODIGOS_MOEDA_PAGAMENTO.map((moeda) => (
                    <option key={moeda} value={moeda}>
                      {moeda}
                    </option>
                  ))}
                </select>
                {formState.errors.codigoMoedaPagamento && (
                  <p className="field-error" role="alert">{formState.errors.codigoMoedaPagamento.message}</p>
                )}
              </div>
            </div>

            <div className="form__group">
              <span className="label">Recebíveis disponíveis</span>
              {isLoading ? (
                <p className="loading" role="status">
                  <span className="loading__spinner" aria-hidden="true" />
                  Carregando…
                </p>
              ) : disponiveis.length === 0 ? (
                <p className="checkbox-group__empty">Nenhum recebível disponível para liquidação.</p>
              ) : (
                <div className="checkbox-group">
                  {disponiveis.map((recebivel) => (
                    <div className="checkbox-item" key={recebivel.id}>
                      <div className="checkbox-row">
                        <label className="checkbox" htmlFor={`recebivel-${recebivel.id}`}>
                          <input id={`recebivel-${recebivel.id}`} type="checkbox" value={String(recebivel.id)} {...register('recebiveisIds')} />
                          <span className="checkbox__text">
                            <span>
                              {recebivel.id} — {recebivel.referenciaExterna} ({recebivel.cedente})
                            </span>
                            <span className="checkbox__meta">
                              {formatMoney(recebivel.valorFace, recebivel.codigoMoeda)} · vence {formatDate(recebivel.dataVencimento)}
                            </span>
                          </span>
                        </label>
                        <button
                          className="btn btn--secondary recebivel-details-button"
                          type="button"
                          data-tooltip="Visualizar detalhes do recebível"
                          aria-label={`Ver detalhes do recebível ${recebivel.referenciaExterna}`}
                          onClick={() => setRecebivelDetalhes((current) => current?.id === recebivel.id ? null : recebivel)}
                        >
                          <svg className={`icon${recebivelDetalhes?.id === recebivel.id ? ' recebivel-expand-icon--open' : ''}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                            <path d="m6 9 6 6 6-6" />
                          </svg>
                        </button>
                      </div>
                      {recebivelDetalhes?.id === recebivel.id && (
                        <section className="recebivel-details" aria-label="Detalhes do recebível">
                          <div className="recebivel-details__header">
                            <h3 className="recebivel-details__title">Detalhes do recebível</h3>
                          </div>
                          <dl className="recebivel-details__list">
                            <div><dt>ID</dt><dd>{recebivel.id}</dd></div>
                            <div><dt>Referência externa</dt><dd>{recebivel.referenciaExterna}</dd></div>
                            <div><dt>Tipo</dt><dd>{recebivel.codigoTipo}</dd></div>
                            <div><dt>Valor de face</dt><dd>{formatMoney(recebivel.valorFace, recebivel.codigoMoeda)}</dd></div>
                            <div><dt>Moeda</dt><dd>{recebivel.codigoMoeda}</dd></div>
                            <div><dt>Vencimento</dt><dd>{formatDate(recebivel.dataVencimento)}</dd></div>
                            <div><dt>Cedente</dt><dd>{recebivel.cedente}</dd></div>
                            <div><dt>Status</dt><dd>{recebivel.status}</dd></div>
                          </dl>
                        </section>
                      )}
                    </div>
                  ))}
                </div>
              )}
              {formState.errors.recebiveisIds && (
                <p className="field-error" role="alert">{formState.errors.recebiveisIds.message}</p>
              )}
            </div>

            <div>
              <button className="btn btn--primary" type="submit" data-tooltip="Liquidar os recebíveis selecionados" disabled={formState.isSubmitting}>
                Liquidar
              </button>
              <ErrorNotice error={error} />
            </div>
          </form>

          {liquidacaoAtual && (
            <LiquidacaoResultado liquidacao={liquidacaoAtual} />
          )}
        </div>
      </section>
    </>
  )
}

function ConsultarLiquidacao() {
  const [resultado, setResultado] = useState<Liquidacao | null>(null)
  const [liquidacoes, setLiquidacoes] = useState<Liquidacao[] | null>(null)
  const [pagina, setPagina] = useState(0)
  const [tamanhoPagina, setTamanhoPagina] = useState(20)
  const [totalPaginas, setTotalPaginas] = useState(0)
  const [error, setError] = useState<unknown>(null)
  const { register, handleSubmit, formState } = useForm<ConsultaForm>({
    resolver: zodResolver(consultaSchema),
  })

  async function onSubmit(values: ConsultaForm) {
    setError(null)
    setResultado(null)
    try {
      setResultado(await obtainLiquidacao(values.id))
    } catch (err) {
      setError(err)
    }
  }

  async function consultarTodas(page = 0, size = tamanhoPagina) {
    setError(null)
    setResultado(null)
    try {
      const result = await listLiquidacoes(page, size)
      setLiquidacoes(result.content)
      setPagina(result.page)
      setTamanhoPagina(result.size)
      setTotalPaginas(result.totalPages)
    } catch (err) {
      setError(err)
    }
  }

  return (
    <section className="card">
      <div className="card__header">
        <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z M21 21l-4.35-4.35" />
        </svg>
        <h2 className="card__title">Consultar liquidação</h2>
      </div>
      <div className="card__body">
        <form className="form" onSubmit={handleSubmit(onSubmit)} noValidate>
          <div className="form__group" style={{ maxWidth: '220px' }}>
            <label className="label" htmlFor="liquidacao-id">
              ID da liquidação
            </label>
            <input id="liquidacao-id" className="input mono" type="number" placeholder="1" {...register('id', { valueAsNumber: true })} />
            {formState.errors.id && <p className="field-error" role="alert">{formState.errors.id.message}</p>}
          </div>
          <div className="liquidacao-consulta-actions">
            <button className="btn btn--primary" type="submit" data-tooltip="Consultar a liquidação pelo ID informado" disabled={formState.isSubmitting}>
              Consultar
            </button>
            <button className="btn btn--secondary" type="button" data-tooltip="Consultar todas as liquidações" onClick={() => void consultarTodas()}>
              Consultar todas as liquidações
            </button>
            {liquidacoes && totalPaginas > 0 && (
              <div className="pagination pagination--inline" aria-label="Paginação das liquidações">
                <button className="btn btn--secondary" type="button" data-tooltip="Consultar página anterior" disabled={pagina === 0} onClick={() => void consultarTodas(pagina - 1)}>
                  Anterior
                </button>
                <span>Página {pagina + 1} de {totalPaginas}</span>
                <button className="btn btn--secondary" type="button" data-tooltip="Consultar próxima página" disabled={pagina + 1 >= totalPaginas} onClick={() => void consultarTodas(pagina + 1)}>
                  Próxima
                </button>
                <label className="pagination__size">
                  Itens por página
                  <select value={tamanhoPagina} onChange={(event) => void consultarTodas(0, Number(event.target.value))}>
                    <option value={20}>20</option>
                    <option value={50}>50</option>
                    <option value={100}>100</option>
                  </select>
                </label>
              </div>
            )}
            <ErrorNotice error={error} />
          </div>
        </form>
        {resultado && (
          <LiquidacaoResultado liquidacao={resultado} />
        )}
        {liquidacoes && (
          <div className="liquidacoes-lista" data-testid="liquidacoes-lista">
            {liquidacoes.length === 0 ? (
              <div className="empty"><p>Nenhuma liquidação encontrada.</p></div>
            ) : liquidacoes.map((liquidacao) => (
              <div className="liquidacao-expandida" key={liquidacao.id}>
                <LiquidacaoResultado liquidacao={liquidacao} />
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  )
}

export default function LiquidacaoPage() {
  return (
    <main className="page">
      <header className="page__header">
        <h1 className="page__title">Liquidação de Lotes</h1>
        <p className="page__subtitle">Liquide lotes de recebíveis e acompanhe o resultado</p>
      </header>
      <CriarLiquidacao />
      <ConsultarLiquidacao />
    </main>
  )
}
