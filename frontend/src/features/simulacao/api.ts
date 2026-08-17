import { http } from '../../lib/api/http'

export interface PrecificacaoResult {
  valorPresente: number
  codigoMoeda: string
  spreadAplicado: number
  prazoMeses: number
  valorLiquido: number
  codigoMoedaPagamento: string
  taxaAplicada: number | null
  vigenciaTaxa: string | null
}

export interface SimulatePrecificacaoRequest {
  codigoTipo: string
  valorFace: number
  codigoMoeda: string
  dataVencimento: string
  codigoMoedaPagamento: string
}

export function simulatePrecificacao(request: SimulatePrecificacaoRequest): Promise<PrecificacaoResult> {
  return http<PrecificacaoResult>('/api/simulacoes/precificacao', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}
