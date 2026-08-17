import { http, HttpResponse } from 'msw'

const accessToken = 'access-token'
const accessTokenExpiresAt = new Date(Date.now() + 15 * 60 * 1000).toISOString()
const refreshToken = 'refresh-token'
const refreshTokenExpiresAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString()

const vigencia = new Date().toISOString()

export const handlers = [
  http.get('*/api/health', () => {
    return HttpResponse.json({ status: 'UP' })
  }),

  http.post('*/api/auth/login', async ({ request }) => {
    const body = (await request.json()) as { username?: string; password?: string }
    if (body.username === 'admin' && body.password === 'admin123') {
      return HttpResponse.json({ accessToken, accessTokenExpiresAt, refreshToken, refreshTokenExpiresAt })
    }
    return HttpResponse.json({ message: 'Invalid username or password.' }, { status: 401 })
  }),

  http.post('*/api/auth/refresh', () => {
    return HttpResponse.json({
      accessToken: 'access-token-new',
      accessTokenExpiresAt,
      refreshToken: 'refresh-token-new',
      refreshTokenExpiresAt,
    })
  }),

  http.post('*/api/auth/logout', () => {
    return new HttpResponse(null, { status: 204 })
  }),

  http.get('*/api/taxas-cambio/vigente', ({ request }) => {
    const url = new URL(request.url)
    return HttpResponse.json({
      codigoBase: url.searchParams.get('codigoBase') ?? 'BRL',
      codigoCotacao: url.searchParams.get('codigoCotacao') ?? 'USD',
      taxa: 0.18,
      vigencia,
    })
  }),

  http.put('*/api/taxas-cambio', async ({ request }) => {
    const body = (await request.json()) as { codigoBase?: string; codigoCotacao?: string; taxa?: number; vigencia?: string }
    return HttpResponse.json({
      codigoBase: body.codigoBase ?? 'BRL',
      codigoCotacao: body.codigoCotacao ?? 'USD',
      taxa: body.taxa ?? 0.18,
      vigencia: body.vigencia ?? vigencia,
    })
  }),

  http.post('*/api/taxas-cambio/integracao', ({ request }) => {
    const url = new URL(request.url)
    return HttpResponse.json({
      codigoBase: url.searchParams.get('codigoBase') ?? 'BRL',
      codigoCotacao: url.searchParams.get('codigoCotacao') ?? 'USD',
      taxa: 0.18,
      vigencia,
    })
  }),

  http.post('*/api/taxas-cambio/convert', async ({ request }) => {
    const body = (await request.json()) as { valor?: number; codigoMoeda?: string }
    return HttpResponse.json({
      valor: (body.valor ?? 100) * 0.18,
      codigoMoeda: body.codigoMoeda ?? 'USD',
      appliedTaxa: 0.18,
      vigencia,
    })
  }),

  http.get('*/api/recebiveis', () => {
    return HttpResponse.json([
      {
        id: 1,
        referenciaExterna: 'REC-001',
        codigoTipo: 'DUPLICATA_MERCANTIL',
        valorFace: 1000,
        codigoMoeda: 'BRL',
        dataVencimento: '2026-09-01',
        cedente: 'Fornecedor A',
        status: 'DISPONIVEL',
      },
    ])
  }),

  http.post('*/api/recebiveis', async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>
    return HttpResponse.json(
      {
        id: 2,
        referenciaExterna: body.referenciaExterna ?? 'REC-002',
        codigoTipo: body.codigoTipo ?? 'DUPLICATA_MERCANTIL',
        valorFace: body.valorFace ?? 0,
        codigoMoeda: body.codigoMoeda ?? 'BRL',
        dataVencimento: body.dataVencimento ?? '2026-09-01',
        cedente: body.cedente ?? 'Fornecedor B',
        status: 'DISPONIVEL',
      },
      { status: 201 },
    )
  }),

  http.post('*/api/simulacoes/precificacao', async ({ request }) => {
    const body = (await request.json()) as { valorFace?: number; codigoMoeda?: string; codigoMoedaPagamento?: string }
    return HttpResponse.json({
      valorPresente: (body.valorFace ?? 1000) * 0.92,
      codigoMoeda: body.codigoMoeda ?? 'BRL',
      spreadAplicado: 0.015,
      prazoMeses: 3,
      valorLiquido: (body.valorFace ?? 1000) * 0.92,
      codigoMoedaPagamento: body.codigoMoedaPagamento ?? 'BRL',
      taxaAplicada: 0.18,
      vigenciaTaxa: vigencia,
    })
  }),

  http.post('*/api/liquidacoes', () => {
    return HttpResponse.json(
      {
        id: 1,
        chaveIdempotencia: '00000000-0000-4000-8000-000000000001',
        status: 'LIQUIDADA',
        createdAt: vigencia,
        itens: [
          {
            recebivelId: 1,
            valorPresente: 920,
            spreadAplicado: 15,
            prazoMeses: 3,
            valorPagamento: 920,
            codigoMoedaPagamento: 'BRL',
            taxaAplicada: 0.18,
          },
        ],
      },
      { status: 201 },
    )
  }),

  http.get('*/api/liquidacoes', () => {
    return HttpResponse.json({
      content: [{
        id: 1,
        chaveIdempotencia: '00000000-0000-4000-8000-000000000001',
        status: 'LIQUIDADA',
        createdAt: vigencia,
        itens: [],
      }],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
  }),

  http.get('*/api/liquidacoes/extrato', () => {
    return HttpResponse.json([
      {
        itemId: 1,
        liquidacaoId: 1,
        chaveIdempotencia: '00000000-0000-4000-8000-000000000001',
        status: 'LIQUIDADA',
        createdAt: vigencia,
        recebivelId: 1,
        cedente: 'Fornecedor A',
        valorPresente: 920,
        spreadAplicado: 15,
        prazoMeses: 3,
        valorPagamento: 920,
        codigoMoedaPagamento: 'BRL',
        taxaAplicada: 0.18,
      },
    ])
  }),

  http.get('*/api/liquidacoes/:id', () => {
    return HttpResponse.json({
      id: 1,
      chaveIdempotencia: '00000000-0000-4000-8000-000000000001',
      status: 'LIQUIDADA',
      createdAt: vigencia,
      itens: [
        {
          recebivelId: 1,
          valorPresente: 920,
          spreadAplicado: 15,
          prazoMeses: 3,
          valorPagamento: 920,
          codigoMoedaPagamento: 'BRL',
          taxaAplicada: 0.18,
        },
      ],
    })
  }),
]
