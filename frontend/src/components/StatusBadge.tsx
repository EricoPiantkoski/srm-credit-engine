interface StatusBadgeProps {
  status: string
}

const STATUS_TONE: Record<string, string> = {
  DISPONIVEL: 'badge--success',
  LIQUIDADO: 'badge--info',
  LIQUIDADA: 'badge--success',
  PROCESSANDO: 'badge--warning',
  FALHOU: 'badge--danger',
  CRIADA: 'badge--neutral',
}

export function StatusBadge({ status }: StatusBadgeProps) {
  const tone = STATUS_TONE[status] ?? 'badge--neutral'
  return <span className={`badge ${tone}`}>{status}</span>
}