import { http } from '../../lib/api/http'

export interface TaxaCambioResponse {
  codigoBase: string
  codigoCotacao: string
  taxa: number
  vigencia: string
}

export interface TaxaCambioUpdateRequest {
  codigoBase: string
  codigoCotacao: string
  taxa: number
  vigencia: string
}

export interface DinheiroConverterRequest {
  valor: number
  codigoMoeda: string
  escala: number
  codigoBase: string
  codigoCotacao: string
}

export interface DinheiroConverterResponse {
  valor: number
  codigoMoeda: string
  appliedTaxa: number
  vigencia: string
}

export function fetchTaxaVigente(codigoBase: string, codigoCotacao: string): Promise<TaxaCambioResponse> {
  const params = new URLSearchParams({ codigoBase, codigoCotacao })
  return http<TaxaCambioResponse>(`/api/taxas-cambio/vigente?${params.toString()}`)
}

export function updateTaxa(request: TaxaCambioUpdateRequest): Promise<TaxaCambioResponse> {
  return http<TaxaCambioResponse>('/api/taxas-cambio', {
    method: 'PUT',
    body: JSON.stringify(request),
  })
}

export function integrateTaxa(codigoBase: string, codigoCotacao: string): Promise<TaxaCambioResponse> {
  const params = new URLSearchParams({ codigoBase, codigoCotacao })
  return http<TaxaCambioResponse>(`/api/taxas-cambio/integracao?${params.toString()}`, { method: 'POST' })
}

export function convertValor(request: DinheiroConverterRequest): Promise<DinheiroConverterResponse> {
  return http<DinheiroConverterResponse>('/api/taxas-cambio/convert', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}