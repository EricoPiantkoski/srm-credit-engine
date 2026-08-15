import { http } from '../../lib/api/http'

export interface HealthResponse {
  status: 'UP'
}

export function fetchHealth(): Promise<HealthResponse> {
  return http<HealthResponse>('/api/health')
}
