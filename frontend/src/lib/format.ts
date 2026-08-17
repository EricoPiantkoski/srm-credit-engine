export function formatMoney(value: number | string, currency = 'BRL'): string {
  const numeric = typeof value === 'string' ? Number(value) : value
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency }).format(numeric)
}

export function formatPercent(value: number | string): string {
  const numeric = typeof value === 'string' ? Number(value) : value
  return new Intl.NumberFormat('pt-BR', {
    style: 'percent',
    minimumFractionDigits: 1,
    maximumFractionDigits: 2,
  }).format(numeric)
}

export function formatInstant(value?: string): string {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString('pt-BR')
}

export function formatDate(value?: string): string {
  if (!value) {
    return '-'
  }
  return new Date(`${value}T00:00:00`).toLocaleDateString('pt-BR')
}
