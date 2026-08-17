import { http } from '../../lib/api/http'

export interface Recebivel {
  id: number
  referenciaExterna: string
  codigoTipo: string
  valorFace: number
  codigoMoeda: string
  dataVencimento: string
  cedente: string
  status: string
}

export interface RecebivelCreateRequest {
  referenciaExterna: string
  codigoTipo: string
  valorFace: number
  codigoMoeda: string
  dataVencimento: string
  cedente: string
}

export const CODIGOS_TIPO = ['DUPLICATA_MERCANTIL', 'CHEQUE_PRE_DATADO'] as const
export const CODIGOS_MOEDA = ['BRL', 'USD'] as const
export const DESCRICOES_MOEDA: Record<(typeof CODIGOS_MOEDA)[number], string> = {
  BRL: 'Real brasileiro',
  USD: 'Dólar americano',
}
export const SPREADS_POR_TIPO: Record<(typeof CODIGOS_TIPO)[number], number> = {
  DUPLICATA_MERCANTIL: 0.015,
  CHEQUE_PRE_DATADO: 0.025,
}

export function formatSpread(codigoTipo: (typeof CODIGOS_TIPO)[number]): string {
  return `${(SPREADS_POR_TIPO[codigoTipo] * 100).toLocaleString('pt-BR', {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  })}% a.m.`
}

export function listRecebiveis(): Promise<Recebivel[]> {
  return http<Recebivel[]>('/api/recebiveis?size=100')
}

export function createRecebivel(request: RecebivelCreateRequest): Promise<Recebivel> {
  return http<Recebivel>('/api/recebiveis', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}
