import { http } from '../../lib/api/http'

export interface ExtratoLiquidacao {
  itemId: number
  liquidacaoId: number
  chaveIdempotencia: string
  status: string
  createdAt: string
  recebivelId: number
  cedente: string
  valorPresente: number
  spreadAplicado: number
  prazoMeses: number
  valorPagamento: number
  codigoMoedaPagamento: string
  taxaAplicada: number | null
}

export interface ExtratoFiltros {
  dataInicial?: string
  dataFinal?: string
  status?: string
  cedente?: string
  codigoMoedaPagamento?: string
  limit?: number
  lastId?: number
}

export const STATUS_LIQUIDACAO = ['PROCESSANDO', 'LIQUIDADA', 'FALHOU'] as const
export const CODIGOS_MOEDA_PAGAMENTO = ['BRL', 'USD'] as const

export function fetchExtrato(filtros: ExtratoFiltros): Promise<ExtratoLiquidacao[]> {
  const params = new URLSearchParams()
  if (filtros.dataInicial) params.set('dataInicial', filtros.dataInicial)
  if (filtros.dataFinal) params.set('dataFinal', filtros.dataFinal)
  if (filtros.status) params.set('status', filtros.status)
  if (filtros.cedente) params.set('cedente', filtros.cedente)
  if (filtros.codigoMoedaPagamento) params.set('codigoMoedaPagamento', filtros.codigoMoedaPagamento)
  if (filtros.limit !== undefined) params.set('limit', String(filtros.limit))
  if (filtros.lastId !== undefined) params.set('lastId', String(filtros.lastId))
  const query = params.toString()
  return http<ExtratoLiquidacao[]>(`/api/liquidacoes/extrato${query ? `?${query}` : ''}`)
}
