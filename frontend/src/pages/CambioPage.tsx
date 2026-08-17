import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { ErrorNotice } from '../components/ErrorNotice'
import {
  convertValor,
  fetchTaxaVigente,
  integrateTaxa,
  updateTaxa,
  type DinheiroConverterResponse,
  type TaxaCambioResponse,
} from '../features/cambio/api'
import { formatInstant, formatMoney } from '../lib/format'

const codigoSchema = z.string().regex(/^[A-Z]{3}$/, 'Código de moeda inválido')
const MOEDAS = ['BRL', 'USD'] as const
type Moeda = (typeof MOEDAS)[number]
const ESCALA_MAXIMA = 18
const NOME_MOEDA: Record<Moeda, string> = {
  BRL: 'BRL | Real',
  USD: 'USD | Dólar Americano',
}

const consultarSchema = z.object({ codigoBase: codigoSchema, codigoCotacao: codigoSchema })
const integrarSchema = z.object({
  codigoBase: z.string().min(1, 'Selecione a moeda de origem'),
  codigoCotacao: z.string().min(1, 'Selecione a moeda de destino'),
}).refine((values) => values.codigoBase !== values.codigoCotacao, {
  path: ['codigoCotacao'],
  message: 'Escolha uma moeda diferente da origem',
})
const inserirSchema = z.object({
  codigoBase: codigoSchema,
  codigoCotacao: codigoSchema,
  taxa: z.number().positive('Taxa deve ser positiva'),
  vigencia: z.string().min(1, 'Vigência obrigatória'),
})
const converterSchema = z.object({
  valor: z.number().positive('Valor deve ser positivo'),
  codigoMoeda: codigoSchema,
  codigoDestino: codigoSchema,
  escala: z.number().int().min(0, 'Precisão mínima 0').max(ESCALA_MAXIMA, `Precisão máxima ${ESCALA_MAXIMA}`),
}).refine((values) => values.codigoMoeda !== values.codigoDestino, {
  path: ['codigoDestino'],
  message: 'Escolha uma moeda de destino diferente da origem',
})

type ConsultarForm = z.infer<typeof consultarSchema>
type InserirForm = z.infer<typeof inserirSchema>
type IntegrarForm = z.infer<typeof integrarSchema>
type ConverterForm = z.infer<typeof converterSchema>

function nowLocalValue(): string {
  const now = new Date()
  now.setSeconds(0, 0)
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60_000)
  return local.toISOString().slice(0, 16)
}

function toInstant(value: string): string {
  return new Date(value).toISOString()
}

function SectionHeader({ icon, title }: { icon: string; title: string }) {
  return (
    <div className="card__header">
      <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d={icon} />
      </svg>
      <h2 className="card__title">{title}</h2>
    </div>
  )
}

const ICON_SEARCH = 'M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z M21 21l-4.35-4.35'
const ICON_EDIT = 'M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7 M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4z'
const ICON_SYNC = 'M21 2v6h-6 M3 12a9 9 0 0 1 15-6.7L21 8 M3 22v-6h6 M21 12a9 9 0 0 1-15 6.7L3 16'
const ICON_CONVERT = 'M17 1l4 4-4 4 M21 5H3 M7 23l-4-4 4-4 M3 19h18'

function TaxaResultado({ resultado }: { resultado: TaxaCambioResponse }) {
  return (
    <div className="rate-hero" data-testid="taxa-resultado">
      <span className="rate-hero__pair">
        {resultado.codigoBase}/{resultado.codigoCotacao}
      </span>
      <span className="rate-hero__value">{resultado.taxa.toLocaleString('pt-BR')}</span>
      <span className="rate-hero__meta">Vigência: {formatInstant(resultado.vigencia)}</span>
    </div>
  )
}

function ConsultarVigente() {
  const [resultado, setResultado] = useState<TaxaCambioResponse | null>(null)
  const [error, setError] = useState<unknown>(null)
  const {
    setValue,
    watch,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ConsultarForm>({
    resolver: zodResolver(consultarSchema),
    defaultValues: { codigoBase: '', codigoCotacao: '' },
  })

  const codigoBase = watch('codigoBase')
  const codigoCotacao = watch('codigoCotacao')

  function selecionarMoeda(campo: 'codigoBase' | 'codigoCotacao', moeda: Moeda) {
    const outroCampo = campo === 'codigoBase' ? 'codigoCotacao' : 'codigoBase'
    const valorDoOutroCampo = campo === 'codigoBase' ? codigoCotacao : codigoBase

    setValue(campo, moeda, { shouldDirty: true, shouldValidate: true })
    if (valorDoOutroCampo === moeda) {
      setValue(outroCampo, '', { shouldDirty: true, shouldValidate: true })
    }
  }

  function moedaEsmaecida(campo: 'codigoBase' | 'codigoCotacao', moeda: Moeda) {
    const valorDaColuna = campo === 'codigoBase' ? codigoBase : codigoCotacao
    const valorDaOutraColuna = campo === 'codigoBase' ? codigoCotacao : codigoBase
    return (valorDaColuna !== '' && valorDaColuna !== moeda) || valorDaOutraColuna === moeda
  }

  async function onSubmit(values: ConsultarForm) {
    setError(null)
    setResultado(null)
    try {
      setResultado(await fetchTaxaVigente(values.codigoBase, values.codigoCotacao))
    } catch (err) {
      setError(err)
    }
  }

  return (
    <section className="card">
      <SectionHeader icon={ICON_SEARCH} title="Consultar taxa vigente" />
      <div className="card__body">
        <form className="form" onSubmit={handleSubmit(onSubmit)} noValidate>
          <div className="form__row">
            <div className="form__group">
              <span className="label">Moeda de origem</span>
              <div className="currency-options" role="group" aria-label="Moeda de origem para consulta">
                {MOEDAS.map((moeda) => (
                  <button
                    className={`btn btn--secondary currency-option${codigoBase === moeda ? ' currency-option--selected' : ''}${moedaEsmaecida('codigoBase', moeda) ? ' currency-option--faded' : ''}`}
                    type="button"
                    key={`consulta-origem-${moeda}`}
                    aria-label={`${moeda} como moeda de origem para consulta`}
                    aria-pressed={codigoBase === moeda}
                    data-tooltip={`Selecionar ${NOME_MOEDA[moeda]} como moeda de origem para consulta`}
                    onClick={() => selecionarMoeda('codigoBase', moeda)}
                  >
                    {NOME_MOEDA[moeda]}
                  </button>
                ))}
              </div>
              {errors.codigoBase && <p className="field-error" role="alert">{errors.codigoBase.message}</p>}
            </div>
            <div className="form__group">
              <span className="label">Moeda de destino</span>
              <div className="currency-options" role="group" aria-label="Moeda de destino para consulta">
                {MOEDAS.map((moeda) => (
                  <button
                    className={`btn btn--secondary currency-option${codigoCotacao === moeda ? ' currency-option--selected' : ''}${moedaEsmaecida('codigoCotacao', moeda) ? ' currency-option--faded' : ''}`}
                    type="button"
                    key={`consulta-destino-${moeda}`}
                    aria-label={`${moeda} como moeda de destino para consulta`}
                    aria-pressed={codigoCotacao === moeda}
                    data-tooltip={`Selecionar ${NOME_MOEDA[moeda]} como moeda de destino para consulta`}
                    onClick={() => selecionarMoeda('codigoCotacao', moeda)}
                  >
                    {NOME_MOEDA[moeda]}
                  </button>
                ))}
              </div>
              {errors.codigoCotacao && <p className="field-error" role="alert">{errors.codigoCotacao.message}</p>}
            </div>
          </div>
          <div className="currency-search-action">
            <button className="btn btn--primary btn--block" type="submit" data-tooltip="Consultar a taxa vigente do par selecionado" disabled={isSubmitting}>
              Consultar cotação
            </button>
            <ErrorNotice error={error} />
          </div>
          {resultado && <TaxaResultado resultado={resultado} />}
        </form>
      </div>
    </section>
  )
}

function InserirTaxa() {
  const [resultado, setResultado] = useState<TaxaCambioResponse | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [isIntegrating, setIsIntegrating] = useState(false)
  const {
    register,
    getValues,
    setValue,
    watch,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<InserirForm>({
    resolver: zodResolver(inserirSchema),
    defaultValues: { vigencia: nowLocalValue() },
  })

  const codigoBase = watch('codigoBase')
  const codigoCotacao = watch('codigoCotacao')

  function selecionarMoeda(campo: 'codigoBase' | 'codigoCotacao', moeda: Moeda) {
    const outroCampo = campo === 'codigoBase' ? 'codigoCotacao' : 'codigoBase'
    const valorDoOutroCampo = campo === 'codigoBase' ? codigoCotacao : codigoBase

    setValue(campo, moeda, { shouldDirty: true, shouldValidate: true })
    if (valorDoOutroCampo === moeda) {
      setValue(outroCampo, '', { shouldDirty: true, shouldValidate: true })
    }
  }

  function moedaEsmaecida(campo: 'codigoBase' | 'codigoCotacao', moeda: Moeda) {
    const valorDaColuna = campo === 'codigoBase' ? codigoBase : codigoCotacao
    const valorDaOutraColuna = campo === 'codigoBase' ? codigoCotacao : codigoBase
    return (valorDaColuna !== '' && valorDaColuna !== moeda) || valorDaOutraColuna === moeda
  }

  async function onSubmit(values: InserirForm) {
    setError(null)
    setResultado(null)
    try {
      const inserida = await updateTaxa({ ...values, vigencia: toInstant(values.vigencia) })
      setResultado(inserida)
    } catch (err) {
      setError(err)
    }
  }

  async function onIntegrar(values: InserirForm) {
    setError(null)
    setResultado(null)
    setIsIntegrating(true)
    try {
      const buscada = await integrateTaxa(values.codigoBase, values.codigoCotacao)
      setResultado(await updateTaxa({
        codigoBase: values.codigoBase,
        codigoCotacao: values.codigoCotacao,
        taxa: buscada.taxa,
        vigencia: toInstant(values.vigencia),
      }))
    } catch (err) {
      setError(err)
    } finally {
      setIsIntegrating(false)
    }
  }

  async function integrarSelecionada() {
    const values = getValues()
    const validation = inserirSchema.safeParse({ ...values, taxa: 1 })
    if (!validation.success) {
      setError(new Error(validation.error.issues[0]?.message ?? 'Preencha os dados da inserção'))
      return
    }
    await onIntegrar(values)
  }

  return (
    <section className="card">
      <SectionHeader icon={ICON_EDIT} title="Inserir cotação manualmente" />
      <div className="card__body">
        <form className="form" onSubmit={handleSubmit(onSubmit)} noValidate>
          <div className="form__row">
            <div className="form__group">
              <span className="label">Moeda de origem</span>
              <div className="currency-options" role="group" aria-label="Moeda de origem para inserção">
                {MOEDAS.map((moeda) => (
                  <button
                    className={`btn btn--secondary currency-option${codigoBase === moeda ? ' currency-option--selected' : ''}${moedaEsmaecida('codigoBase', moeda) ? ' currency-option--faded' : ''}`}
                    type="button"
                    key={`inserir-origem-${moeda}`}
                    aria-label={`${moeda} como moeda de origem para inserção`}
                    aria-pressed={codigoBase === moeda}
                    data-tooltip={`Selecionar ${NOME_MOEDA[moeda]} como moeda de origem para inserção`}
                    onClick={() => selecionarMoeda('codigoBase', moeda)}
                  >
                    {NOME_MOEDA[moeda]}
                  </button>
                ))}
              </div>
              {errors.codigoBase && <p className="field-error" role="alert">{errors.codigoBase.message}</p>}
            </div>
            <div className="form__group">
              <span className="label">Moeda de destino</span>
              <div className="currency-options" role="group" aria-label="Moeda de destino para inserção">
                {MOEDAS.map((moeda) => (
                  <button
                    className={`btn btn--secondary currency-option${codigoCotacao === moeda ? ' currency-option--selected' : ''}${moedaEsmaecida('codigoCotacao', moeda) ? ' currency-option--faded' : ''}`}
                    type="button"
                    key={`inserir-destino-${moeda}`}
                    aria-label={`${moeda} como moeda de destino para inserção`}
                    aria-pressed={codigoCotacao === moeda}
                    data-tooltip={`Selecionar ${NOME_MOEDA[moeda]} como moeda de destino para inserção`}
                    onClick={() => selecionarMoeda('codigoCotacao', moeda)}
                  >
                    {NOME_MOEDA[moeda]}
                  </button>
                ))}
              </div>
              {errors.codigoCotacao && <p className="field-error" role="alert">{errors.codigoCotacao.message}</p>}
            </div>
          </div>
          <div className="form__row">
            <div className="form__group">
              <label className="label" htmlFor="taxa">
                Cotação
              </label>
              <input id="taxa" className="input" type="number" step="any" inputMode="decimal" placeholder="5.25" {...register('taxa', { valueAsNumber: true })} />
              <p className="field-hint">Use ponto (.) como separador decimal.</p>
              {errors.taxa && <p className="field-error" role="alert">{errors.taxa.message}</p>}
            </div>
            <div className="form__group">
              <label className="label" htmlFor="vigencia">
                Vigência
              </label>
              <input id="vigencia" className="input" type="datetime-local" {...register('vigencia')} />
              {errors.vigencia && <p className="field-error" role="alert">{errors.vigencia.message}</p>}
            </div>
          </div>
          <div>
            <button className="btn btn--primary" type="submit" data-tooltip="Inserir manualmente a cotação informada" disabled={isSubmitting || isIntegrating}>
              Inserir
            </button>
            <button className="btn btn--secondary" type="button" data-tooltip="Busque pela cotação e insira-a com a vigência da inserção manual" onClick={() => void integrarSelecionada()} disabled={isSubmitting || isIntegrating}>
              Inserir cotação integração
            </button>
            <ErrorNotice error={error} />
          </div>
          {resultado && <TaxaResultado resultado={resultado} />}
        </form>
      </div>
    </section>
  )
}

function IntegrarTaxa() {
  const [resultado, setResultado] = useState<TaxaCambioResponse | null>(null)
  const [error, setError] = useState<unknown>(null)
  const {
    setValue,
    watch,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<IntegrarForm>({
    resolver: zodResolver(integrarSchema),
    defaultValues: { codigoBase: '', codigoCotacao: '' },
  })

  const codigoBase = watch('codigoBase')
  const codigoCotacao = watch('codigoCotacao')

  function selecionarMoeda(campo: 'codigoBase' | 'codigoCotacao', moeda: Moeda) {
    const outroCampo = campo === 'codigoBase' ? 'codigoCotacao' : 'codigoBase'
    const valorDoOutroCampo = campo === 'codigoBase' ? codigoCotacao : codigoBase

    setValue(campo, moeda, { shouldDirty: true, shouldValidate: true })
    if (valorDoOutroCampo === moeda) {
      setValue(outroCampo, '', { shouldDirty: true, shouldValidate: true })
    }
  }

  function moedaEsmaecida(campo: 'codigoBase' | 'codigoCotacao', moeda: Moeda) {
    const valorDaColuna = campo === 'codigoBase' ? codigoBase : codigoCotacao
    const valorDaOutraColuna = campo === 'codigoBase' ? codigoCotacao : codigoBase
    return (valorDaColuna !== '' && valorDaColuna !== moeda) || valorDaOutraColuna === moeda
  }

  async function onSubmit(values: IntegrarForm) {
    setError(null)
    setResultado(null)
    try {
      setResultado(await integrateTaxa(values.codigoBase, values.codigoCotacao))
    } catch (err) {
      setError(err)
    }
  }

  return (
    <section className="card">
      <SectionHeader icon={ICON_SYNC} title="Buscar Cotação Moeda" />
      <div className="card__body">
        <form className="form" onSubmit={handleSubmit(onSubmit)} noValidate>
          <div className="form__row">
            <div className="form__group">
              <span className="label">Moeda de origem</span>
              <div className="currency-options" role="group" aria-label="Moeda de origem">
                {MOEDAS.map((moeda) => (
                  <button
                    className={`btn btn--secondary currency-option${codigoBase === moeda ? ' currency-option--selected' : ''}${moedaEsmaecida('codigoBase', moeda) ? ' currency-option--faded' : ''}`}
                    type="button"
                    key={`origem-${moeda}`}
                    aria-label={`${moeda} como moeda de origem`}
                    aria-pressed={codigoBase === moeda}
                    data-tooltip={`Selecionar ${NOME_MOEDA[moeda]} como moeda de origem`}
                    onClick={() => selecionarMoeda('codigoBase', moeda)}
                  >
                    {NOME_MOEDA[moeda]}
                  </button>
                ))}
              </div>
              {errors.codigoBase && <p className="field-error" role="alert">{errors.codigoBase.message}</p>}
            </div>
            <div className="form__group">
              <span className="label">Moeda de destino</span>
              <div className="currency-options" role="group" aria-label="Moeda de destino">
                {MOEDAS.map((moeda) => (
                  <button
                    className={`btn btn--secondary currency-option${codigoCotacao === moeda ? ' currency-option--selected' : ''}${moedaEsmaecida('codigoCotacao', moeda) ? ' currency-option--faded' : ''}`}
                    type="button"
                    key={`destino-${moeda}`}
                    aria-label={`${moeda} como moeda de destino`}
                    aria-pressed={codigoCotacao === moeda}
                    data-tooltip={`Selecionar ${NOME_MOEDA[moeda]} como moeda de destino`}
                    onClick={() => selecionarMoeda('codigoCotacao', moeda)}
                  >
                    {NOME_MOEDA[moeda]}
                  </button>
                ))}
              </div>
              {errors.codigoCotacao && <p className="field-error" role="alert">{errors.codigoCotacao.message}</p>}
            </div>
          </div>
          <div className="currency-search-action">
            <button className="btn btn--primary btn--block" type="submit" data-tooltip="Buscar a cotação no provedor externo" disabled={isSubmitting}>
              Buscar
            </button>
            <ErrorNotice error={error} />
          </div>
          {resultado && <TaxaResultado resultado={resultado} />}
        </form>
      </div>
    </section>
  )
}

function ConverterValor() {
  const [resultado, setResultado] = useState<DinheiroConverterResponse | null>(null)
  const [cotacao, setCotacao] = useState<TaxaCambioResponse | null>(null)
  const [error, setError] = useState<unknown>(null)
  const {
    register,
    setValue,
    watch,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ConverterForm>({
    resolver: zodResolver(converterSchema),
    defaultValues: { escala: 2 },
  })

  const codigoMoeda = watch('codigoMoeda')
  const codigoDestino = watch('codigoDestino')

  async function selecionarMoeda(campo: 'codigoMoeda' | 'codigoDestino', moeda: Moeda) {
    const outroCampo = campo === 'codigoMoeda' ? 'codigoDestino' : 'codigoMoeda'
    const valorDoOutroCampo = campo === 'codigoMoeda' ? codigoDestino : codigoMoeda
    const novaMoedaOrigem = campo === 'codigoMoeda' ? moeda : codigoMoeda
    const novaMoedaDestino = campo === 'codigoDestino' ? moeda : codigoDestino

    setValue(campo, moeda, { shouldDirty: true, shouldValidate: true })
    if (valorDoOutroCampo === moeda) {
      setValue(outroCampo, '', { shouldDirty: true, shouldValidate: true })
      setCotacao(null)
      return
    }

    if (novaMoedaOrigem && novaMoedaDestino) {
      setError(null)
      setCotacao(null)
      try {
        setCotacao(await fetchTaxaVigente(novaMoedaOrigem, novaMoedaDestino))
      } catch (err) {
        setError(err)
      }
    }
  }

  function moedaEsmaecida(campo: 'codigoMoeda' | 'codigoDestino', moeda: Moeda) {
    const valorDaColuna = campo === 'codigoMoeda' ? codigoMoeda : codigoDestino
    const valorDaOutraColuna = campo === 'codigoMoeda' ? codigoDestino : codigoMoeda
    return (valorDaColuna !== '' && valorDaColuna !== moeda) || valorDaOutraColuna === moeda
  }

  async function onSubmit(values: ConverterForm) {
    setError(null)
    setResultado(null)
    try {
      setResultado(await convertValor({
        valor: values.valor,
        codigoMoeda: values.codigoMoeda,
        escala: values.escala ?? 2,
        codigoBase: values.codigoMoeda,
        codigoCotacao: values.codigoDestino,
      }))
    } catch (err) {
      setError(err)
    }
  }

  return (
    <section className="card">
      <SectionHeader icon={ICON_CONVERT} title="Converter" />
      <div className="card__body">
        <form className="form" onSubmit={handleSubmit(onSubmit)} noValidate>
          <div className="form__row converter-value-row">
            <div className="form__group converter-value">
              <label className="label" htmlFor="converter-valor">
                Valor
              </label>
              <input id="converter-valor" className="input" type="number" step="any" placeholder="100,00" {...register('valor', { valueAsNumber: true })} />
              {errors.valor && <p className="field-error" role="alert">{errors.valor.message}</p>}
            </div>
          </div>
          <div className="form__row">
            <div className="form__group">
              <span className="label">Moeda a converter</span>
              <div className="currency-options" role="group" aria-label="Moeda a converter">
                {MOEDAS.map((moeda) => (
                  <button
                    className={`btn btn--secondary currency-option${codigoMoeda === moeda ? ' currency-option--selected' : ''}${moedaEsmaecida('codigoMoeda', moeda) ? ' currency-option--faded' : ''}`}
                    type="button"
                    key={`converter-origem-${moeda}`}
                    aria-label={`${moeda} como moeda a converter`}
                    aria-pressed={codigoMoeda === moeda}
                    data-tooltip={`Selecionar ${NOME_MOEDA[moeda]} como moeda a converter`}
                    onClick={() => void selecionarMoeda('codigoMoeda', moeda)}
                  >
                    {NOME_MOEDA[moeda]}
                  </button>
                ))}
              </div>
              {errors.codigoMoeda && <p className="field-error" role="alert">{errors.codigoMoeda.message}</p>}
            </div>
            <div className="form__group">
              <span className="label">Moeda de destino</span>
              <div className="currency-options" role="group" aria-label="Moeda de destino para conversão">
                {MOEDAS.map((moeda) => (
                  <button
                    className={`btn btn--secondary currency-option${codigoDestino === moeda ? ' currency-option--selected' : ''}${moedaEsmaecida('codigoDestino', moeda) ? ' currency-option--faded' : ''}`}
                    type="button"
                    key={`converter-destino-${moeda}`}
                    aria-label={`${moeda} como moeda de destino para conversão`}
                    aria-pressed={codigoDestino === moeda}
                    data-tooltip={`Selecionar ${NOME_MOEDA[moeda]} como moeda de destino para conversão`}
                    onClick={() => void selecionarMoeda('codigoDestino', moeda)}
                  >
                    {NOME_MOEDA[moeda]}
                  </button>
                ))}
              </div>
              {errors.codigoDestino && <p className="field-error" role="alert">{errors.codigoDestino.message}</p>}
            </div>
          </div>
          {cotacao && <TaxaResultado resultado={cotacao} />}
          <div className="form__group" style={{ maxWidth: '180px' }}>
            <label className="label" htmlFor="converter-escala">
              Precisão decimal
            </label>
            <input id="converter-escala" className="input" type="number" min="0" max={ESCALA_MAXIMA} step="1" {...register('escala', { valueAsNumber: true })} />
            {errors.escala && <p className="field-error" role="alert">{errors.escala.message}</p>}
          </div>
          <div>
            <button className="btn btn--primary" type="submit" data-tooltip="Converter o valor usando a cotação selecionada" disabled={isSubmitting}>
              Converter
            </button>
            <ErrorNotice error={error} />
          </div>
          {resultado && (
            <div className="rate-hero" data-testid="conversao-resultado">
              <span className="rate-hero__value">{formatMoney(resultado.valor, resultado.codigoMoeda)}</span>
            </div>
          )}
        </form>
      </div>
    </section>
  )
}

export default function CambioPage() {
  return (
    <main className="page">
      <header className="page__header">
        <h1 className="page__title">Operação de Câmbio</h1>
        <p className="page__subtitle">Consultas, inserções e integrações de taxas de câmbio</p>
      </header>
      <div className="grid--2">
        <ConsultarVigente />
        <InserirTaxa />
      </div>
      <div className="grid--2" style={{ marginTop: '1.25rem' }}>
        <IntegrarTaxa />
        <ConverterValor />
      </div>
    </main>
  )
}
