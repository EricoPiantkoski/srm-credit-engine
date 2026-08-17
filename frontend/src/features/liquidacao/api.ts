import { http } from '../../lib/api/http'

export interface ItemLiquidacao {
  recebivelId: number
  valorPresente: number
  spreadAplicado: number
  prazoMeses: number
  valorPagamento: number
  codigoMoedaPagamento: string
  taxaAplicada: number
}

export interface Liquidacao {
  id: number
  chaveIdempotencia: string
  status: string
  createdAt: string
  itens: ItemLiquidacao[]
}

export interface LiquidacaoPage {
  content: Liquidacao[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface LiquidacaoCreateRequest {
  chaveIdempotencia: string
  codigoMoedaPagamento: string
  recebiveisIds: number[]
}

export function createLiquidacao(request: LiquidacaoCreateRequest): Promise<Liquidacao> {
  return http<Liquidacao>('/api/liquidacoes', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function obtainLiquidacao(id: number): Promise<Liquidacao> {
  return http<Liquidacao>(`/api/liquidacoes/${id}`)
}

export function listLiquidacoes(page = 0, size = 20): Promise<LiquidacaoPage> {
  return http<LiquidacaoPage>(`/api/liquidacoes?page=${page}&size=${size}`)
}
