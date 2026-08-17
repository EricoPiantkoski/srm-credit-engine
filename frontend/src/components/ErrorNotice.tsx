import { getErrorMessage } from '../lib/api/errors'

interface ErrorNoticeProps {
  error: unknown
  fallback?: string
  statusMessages?: Partial<Record<number, string>>
}

export function ErrorNotice({ error, fallback, statusMessages }: ErrorNoticeProps) {
  if (!error) {
    return null
  }
  return (
    <p className="alert alert--error" role="alert">
      {getErrorMessage(error, fallback, statusMessages)}
    </p>
  )
}
