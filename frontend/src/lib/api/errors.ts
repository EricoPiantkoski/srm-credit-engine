import { ApiError } from './http'

const STATUS_MESSAGES: Record<number, string> = {
  400: 'Requisição inválida. Verifique os dados informados.',
  401: 'Sessão expirada. Faça login novamente.',
  403: 'Acesso negado.',
  404: 'Nenhum registro encontrado.',
  409: 'Conflito: o recurso já existe ou foi alterado por outra operação.',
  422: 'Não foi possível processar os dados informados.',
  429: 'Limite de requisições excedido. Aguarde alguns instantes e tente novamente.',
  500: 'Erro interno do servidor.',
  503: 'Serviço indisponível no momento. Tente novamente mais tarde.',
}

export function getErrorMessage(
  error: unknown,
  fallback = 'Ocorreu um erro inesperado.',
  statusMessages: Partial<Record<number, string>> = {},
): string {
  if (error instanceof ApiError) {
    return statusMessages[error.status] ?? STATUS_MESSAGES[error.status] ?? fallback
  }
  return fallback
}
