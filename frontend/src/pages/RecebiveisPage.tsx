import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Fragment, useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { ErrorNotice } from '../components/ErrorNotice'
import { StatusBadge } from '../components/StatusBadge'
import {
  CODIGOS_MOEDA,
  CODIGOS_TIPO,
  createRecebivel,
  DESCRICOES_MOEDA,
  formatSpread,
  listRecebiveis,
  type Recebivel,
} from '../features/recebiveis/api'
import { formatDate, formatMoney } from '../lib/format'

const recebivelSchema = z.object({
  referenciaExterna: z.string().min(1, 'Referência é obrigatória'),
  codigoTipo: z.enum(CODIGOS_TIPO, { message: 'Selecione o tipo' }),
  valorFace: z.number().positive('Valor deve ser positivo'),
  codigoMoeda: z.enum(CODIGOS_MOEDA, { message: 'Selecione a moeda' }),
  dataVencimento: z.string().min(1, 'Vencimento é obrigatório'),
  cedente: z.string().min(1, 'Cedente é obrigatório'),
})

type RecebivelForm = z.infer<typeof recebivelSchema>

export default function RecebiveisPage() {
  const queryClient = useQueryClient()
  const [error, setError] = useState<unknown>(null)
  const [recebivelDetalhes, setRecebivelDetalhes] = useState<Recebivel | null>(null)
  const [mostrarTodosDetalhes, setMostrarTodosDetalhes] = useState(false)
  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<RecebivelForm>({ resolver: zodResolver(recebivelSchema) })

  const codigoTipo = watch('codigoTipo')
  const codigoMoeda = watch('codigoMoeda')

  const { data, isLoading, error: listError } = useQuery({
    queryKey: ['recebiveis'],
    queryFn: listRecebiveis,
  })

  const mutation = useMutation({
    mutationFn: createRecebivel,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recebiveis'] })
    },
  })

  async function onSubmit(values: RecebivelForm) {
    setError(null)
    try {
      await mutation.mutateAsync(values)
      reset()
    } catch (err) {
      setError(err)
    }
  }

  return (
    <main className="page">
      <header className="page__header">
        <h1 className="page__title">Recebíveis</h1>
        <p className="page__subtitle">Cadastro e consulta de recebíveis do portfólio</p>
      </header>

      <section className="card">
        <div className="card__header">
          <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M12 5v14 M5 12h14" />
          </svg>
          <h2 className="card__title">Cadastrar recebível</h2>
        </div>
        <div className="card__body">
          <form className="form" onSubmit={handleSubmit(onSubmit)} noValidate>
            <div className="form__row">
              <div className="form__group">
                <label className="label" htmlFor="referenciaExterna">
                  Referência externa
                </label>
                <input id="referenciaExterna" className="input" placeholder="REC-001" {...register('referenciaExterna')} />
                {errors.referenciaExterna && <p className="field-error" role="alert">{errors.referenciaExterna.message}</p>}
              </div>
              <div className="form__group form__group--descriptive">
                <label className="label" htmlFor="codigoTipo">
                  Tipo
                </label>
                <select id="codigoTipo" className="select" {...register('codigoTipo')}>
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
            </div>

            <div className="form__row">
              <div className="form__group">
                <label className="label" htmlFor="valorFace">
                  Valor de face
                </label>
                <input id="valorFace" className="input" type="number" step="any" placeholder="0,00" {...register('valorFace', { valueAsNumber: true })} />
                {errors.valorFace && <p className="field-error" role="alert">{errors.valorFace.message}</p>}
              </div>
              <div className="form__group form__group--descriptive">
                <label className="label" htmlFor="codigoMoeda">
                  Moeda
                </label>
                <select id="codigoMoeda" className="select" {...register('codigoMoeda')}>
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
            </div>

            <div className="form__row">
              <div className="form__group">
                <label className="label" htmlFor="dataVencimento">
                  Vencimento
                </label>
                <input id="dataVencimento" className="input" type="date" {...register('dataVencimento')} />
                {errors.dataVencimento && <p className="field-error" role="alert">{errors.dataVencimento.message}</p>}
              </div>
              <div className="form__group">
                <label className="label" htmlFor="cedente">
                  Cedente
                </label>
                <input id="cedente" className="input" placeholder="Fornecedor A" {...register('cedente')} />
                {errors.cedente && <p className="field-error" role="alert">{errors.cedente.message}</p>}
              </div>
            </div>

            <div>
              <button className="btn btn--primary" type="submit" data-tooltip="Cadastrar o recebível" disabled={isSubmitting}>
                Cadastrar
              </button>
            </div>

            {mutation.isSuccess && (
              <p className="alert alert--success" role="status">
                Recebível {mutation.data.referenciaExterna} criado.
              </p>
            )}
            <ErrorNotice error={error} />
          </form>
        </div>
      </section>

      <section className="card">
        <div className="card__header">
          <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M8 6h13 M8 12h13 M8 18h13 M3 6h.01 M3 12h.01 M3 18h.01" />
          </svg>
          <h2 className="card__title">Lista de recebíveis</h2>
          <button
            className="btn btn--secondary recebiveis-consultar-todos"
            type="button"
            data-tooltip="Visualizar detalhes de todos os recebíveis disponíveis"
            aria-label="Consultar todos os recebíveis disponíveis"
            onClick={() => {
              setMostrarTodosDetalhes((current) => !current)
              setRecebivelDetalhes(null)
            }}
          >
            <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M11 19a8 8 0 1 1 0-16 8 8 0 0 1 0 16z M21 21l-4.35-4.35" />
            </svg>
            Consultar todas
          </button>
        </div>
        <div className="card__body">
          {isLoading ? (
            <p className="loading" role="status">
              <span className="loading__spinner" aria-hidden="true" />
              Carregando…
            </p>
          ) : data?.length === 0 ? (
            <div className="empty">
              <span className="empty__icon" aria-hidden="true">∅</span>
              <p>Nenhum recebível cadastrado ainda.</p>
            </div>
          ) : (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Ref.</th>
                    <th>Tipo</th>
                    <th>Valor de face</th>
                    <th>Moeda</th>
                    <th>Vencimento</th>
                    <th>Cedente</th>
                    <th>Status</th>
                    <th>Ações</th>
                  </tr>
                </thead>
                <tbody>
                   {data?.map((recebivel) => (
                     <Fragment key={recebivel.id}>
                     <tr>
                      <td className="mono">{recebivel.referenciaExterna}</td>
                      <td>{recebivel.codigoTipo}</td>
                      <td className="mono">{formatMoney(recebivel.valorFace, recebivel.codigoMoeda)}</td>
                      <td>{recebivel.codigoMoeda}</td>
                      <td>{formatDate(recebivel.dataVencimento)}</td>
                      <td>{recebivel.cedente}</td>
                      <td>
                        <StatusBadge status={recebivel.status} />
                      </td>
                      <td>
                        <button
                          className="btn btn--secondary recebivel-details-button"
                          type="button"
                          data-tooltip="Visualizar detalhes do recebível"
                          aria-label={`Ver detalhes do recebível ${recebivel.referenciaExterna}`}
                          onClick={() => {
                            setMostrarTodosDetalhes(false)
                            setRecebivelDetalhes((current) => current?.id === recebivel.id ? null : recebivel)
                          }}
                        >
                          <svg className={`icon${(mostrarTodosDetalhes || recebivelDetalhes?.id === recebivel.id) ? ' recebivel-expand-icon--open' : ''}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                            <path d="m6 9 6 6 6-6" />
                          </svg>
                        </button>
                      </td>
                     </tr>
                      {(mostrarTodosDetalhes || recebivelDetalhes?.id === recebivel.id) && (
                        <tr>
                          <td colSpan={8}>
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
                          </td>
                        </tr>
                      )}
                     </Fragment>
                   ))}
                </tbody>
              </table>
            </div>
          )}
          <ErrorNotice error={listError} />
        </div>
      </section>
    </main>
  )
}
