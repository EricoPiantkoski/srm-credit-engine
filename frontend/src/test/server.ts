import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'

export const handlers = [
  http.get('*/api/health', () => {
    return HttpResponse.json({ status: 'UP' })
  }),
]

export const server = setupServer(...handlers)
