import { useHealth } from '../features/health/hooks'

export default function HomePage() {
  const { data, error, isLoading } = useHealth()

  if (isLoading) {
    return <p role="status">Carregando…</p>
  }

  if (error) {
    return <p role="alert">Não foi possível conectar ao serviço.</p>
  }

  return (
    <main>
      <h1>SRM Credit Engine</h1>
      <p>Status do serviço: {data?.status}</p>
    </main>
  )
}
