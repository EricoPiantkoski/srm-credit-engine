import { Link } from 'react-router-dom'

import { useHealth } from '../features/health/hooks'

const panels = [
  {
    to: '/cambio',
    label: 'Câmbio',
    description: 'Consultar, inserir e integrar taxas; converter valores.',
    path: 'M3 7h13l3 3v7a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V8a1 1 0 0 1 1-1z M3 7V5a1 1 0 0 1 1-1h9l2 3',
  },
  {
    to: '/recebiveis',
    label: 'Recebíveis',
    description: 'Cadastrar e consultar recebíveis.',
    path: 'M8 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8z M4 20c0-2.2 1.8-4 4-4s4 1.8 4 4 M16 11h6 M19 8v6',
  },
  {
    to: '/simulacao',
    label: 'Simulação',
    description: 'Simular a precificação de ativos.',
    path: 'M3 3v18h18 M7 15l4-6 4 3 5-7',
  },
  {
    to: '/liquidacoes',
    label: 'Liquidação',
    description: 'Liquidar lotes de recebíveis.',
    path: 'M5 3h14a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z M9 8h6 M9 12h6 M9 16h3',
  },
  {
    to: '/extrato',
    label: 'Extrato',
    description: 'Consultar o extrato de liquidações.',
    path: 'M6 2h9l5 5v15a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1z M14 2v6h6 M9 13h6 M9 17h6',
  },
]

function PanelIcon({ path }: { path: string }) {
  return (
    <svg
      className="panel-card__icon"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d={path} />
    </svg>
  )
}

export default function HomePage() {
  const { data, error, isLoading } = useHealth()

  return (
    <main className="page">
      <header className="page__header">
        <div className="page__header-top">
          <h1 className="page__title">SRM Credit Engine</h1>
          {isLoading ? (
            <span className="service-status service-status--loading" role="status">
              <span className="loading__spinner" style={{ width: '12px', height: '12px' }} aria-hidden="true" />
              Carregando status…
            </span>
          ) : error ? (
            <span className="service-status service-status--down" role="alert">
              Status do serviço: indisponível
            </span>
          ) : (
            <span className="service-status">
              <span className="status-dot" aria-hidden="true" />
              Status do serviço: {data?.status}
            </span>
          )}
        </div>
        <p className="page__subtitle">Painel de operações de recebíveis</p>
      </header>

      <nav aria-label="Painéis de operação">
        <div className="panel-grid">
          {panels.map((panel) => (
            <Link key={panel.to} to={panel.to} className="panel-card">
              <PanelIcon path={panel.path} />
              <span className="panel-card__label">{panel.label}</span>
              <span className="panel-card__desc">{panel.description}</span>
            </Link>
          ))}
        </div>
      </nav>
    </main>
  )
}