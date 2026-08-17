import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { ErrorNotice } from '../components/ErrorNotice'
import { simulatePrecificacao, type PrecificacaoResult } from '../features/simulacao/api'
import { CODIGOS_MOEDA, CODIGOS_TIPO, DESCRICOES_MOEDA, formatSpread } from '../features/recebiveis/api'
import { formatInstant, formatMoney, formatPercent } from '../lib/format'

const simulacaoSchema = z.object({
  codigoTipo: z.enum(CODIGOS_TIPO, { message: 'Selecione o tipo' }),
  valorFace: z.number().positive('Valor deve ser positivo'),
  codigoMoeda: z.enum(CODIGOS_MOEDA, { message: 'Selecione a moeda' }),
  dataVencimento: z.string().min(1, 'Vencimento é obrigatório'),
  codigoMoedaPagamento: z.enum(CODIGOS_MOEDA, { message: 'Selecione a moeda de pagamento' }),
})

type SimulacaoForm = z.infer<typeof simulacaoSchema>

export default function SimulacaoPage() {
  const [resultado, setResultado] = useState<PrecificacaoResult | null>(null)
  const [error, setError] = useState<unknown>(null)
  const {
    register,
    watch,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<SimulacaoForm>({ resolver: zodResolver(simulacaoSchema) })

  const codigoTipo = watch('codigoTipo')
  const codigoMoeda = watch('codigoMoeda')
  const codigoMoedaPagamento = watch('codigoMoedaPagamento')

  async function onSubmit(values: SimulacaoForm) {
    setError(null)
    setResultado(null)
    try {
      setResultado(await simulatePrecificacao(values))
    } catch (err) {
      setError(err)
    }
  }

  return (
    <main className="page">
      <header className="page__header">
        <h1 className="page__title">Simulação de Precificação</h1>
        <p className="page__subtitle">Projete o valor líquido de antecipação sem persistir dados</p>
      </header>

      <section className="card">
        <div className="card__header">
          <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M3 3v18h18 M7 15l4-6 4 3 5-7" />
          </svg>
          <h2 className="card__title">Parâmetros da simulação</h2>
        </div>
        <div className="card__body">
          <form className="form" onSubmit={handleSubmit(onSubmit)} noValidate>
            <div className="form__row">
              <div className="form__group form__group--descriptive">
                <label className="label" htmlFor="simulacao-tipo">
                  Tipo
                </label>
                <select id="simulacao-tipo" className="select" {...register('codigoTipo')}>
                  <option value="">Selecione…</option>
                  {CODIGOS_TIPO.map((tipo) => (
                    <option key={tipo} value={tipo}>
                      {tipo}
                    </option>
                  ))}
                </select>
                {codigoTipo && <p className="field-hint field-hint--spread">Spread: {formatSpread(codigoTipo)}</p>}
                {errors.codigoTipo && <p className="field-error" role="alert">{errors.codigoTipo.message}</p>}
              </div>
              <div className="form__group">
                <label className="label" htmlFor="simulacao-valor">
                  Valor de face
                </label>
                <input id="simulacao-valor" className="input" type="number" step="any" placeholder="0,00" {...register('valorFace', { valueAsNumber: true })} />
                {errors.valorFace && <p className="field-error" role="alert">{errors.valorFace.message}</p>}
              </div>
            </div>

            <div className="form__row">
              <div className="form__group form__group--descriptive">
                <label className="label" htmlFor="simulacao-moeda">
                  Moeda
                </label>
                <select id="simulacao-moeda" className="select" {...register('codigoMoeda')}>
                  <option value="">Selecione…</option>
                  {CODIGOS_MOEDA.map((moeda) => (
                    <option key={moeda} value={moeda}>
                      {moeda}
                    </option>
                  ))}
              </select>
              {codigoMoeda && <p className="field-hint field-hint--currency">{DESCRICOES_MOEDA[codigoMoeda]}</p>}
              {errors.codigoMoeda && <p className="field-error" role="alert">{errors.codigoMoeda.message}</p>}
              </div>
              <div className="form__group">
                <label className="label" htmlFor="simulacao-vencimento">
                  Vencimento
                </label>
                <input id="simulacao-vencimento" className="input" type="date" {...register('dataVencimento')} />
                {errors.dataVencimento && <p className="field-error" role="alert">{errors.dataVencimento.message}</p>}
              </div>
            </div>

            <div className="form__group form__group--descriptive" style={{ maxWidth: '50%' }}>
              <label className="label" htmlFor="simulacao-pagamento">
                Moeda de pagamento
              </label>
              <select id="simulacao-pagamento" className="select" {...register('codigoMoedaPagamento')}>
                <option value="">Selecione…</option>
                {CODIGOS_MOEDA.map((moeda) => (
                  <option key={moeda} value={moeda}>
                    {moeda}
                  </option>
                ))}
              </select>
              {codigoMoedaPagamento && <p className="field-hint field-hint--currency">{DESCRICOES_MOEDA[codigoMoedaPagamento]}</p>}
              {errors.codigoMoedaPagamento && <p className="field-error" role="alert">{errors.codigoMoedaPagamento.message}</p>}
            </div>

            <div>
              <button className="btn btn--primary" type="submit" data-tooltip="Simular a precificação do recebível" disabled={isSubmitting}>
                Simular
              </button>
              <ErrorNotice error={error} />
            </div>
          </form>
        </div>
      </section>

      {resultado && (
        <section className="result-card" aria-label="Resultado da simulação" data-testid="simulacao-resultado">
          <div className="result-card__header">
            <h2 className="result-card__title">Resultado da simulação</h2>
            <StatusPill codigoMoeda={resultado.codigoMoeda} />
          </div>
          <dl className="result-list">
            <div className="dl-row">
              <dt>Valor presente</dt>
              <dd>{formatMoney(resultado.valorPresente, resultado.codigoMoeda)}</dd>
            </div>
            <div className="dl-row">
              <dt>Spread aplicado</dt>
              <dd>{formatPercent(resultado.spreadAplicado)}</dd>
            </div>
            <div className="dl-row">
              <dt>Prazo (meses)</dt>
              <dd>{Math.trunc(resultado.prazoMeses)}</dd>
            </div>
            <div className="dl-row">
              <dt>Taxa de câmbio</dt>
              <dd>{resultado.taxaAplicada == null ? '-' : resultado.taxaAplicada.toLocaleString('pt-BR')}</dd>
            </div>
            <div className="dl-row">
              <dt>Vigência da taxa</dt>
              <dd>{resultado.vigenciaTaxa ? formatInstant(resultado.vigenciaTaxa) : '-'}</dd>
            </div>
            <div className="dl-row">
              <dt>Valor líquido</dt>
              <dd className="highlight">{formatMoney(resultado.valorLiquido, resultado.codigoMoedaPagamento)}</dd>
            </div>
          </dl>
        </section>
      )}
    </main>
  )
}

function StatusPill({ codigoMoeda }: { codigoMoeda: string }) {
  return <span className="badge badge--info">{codigoMoeda}</span>
}
