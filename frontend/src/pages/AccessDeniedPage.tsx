import { Link } from 'react-router-dom'

export default function AccessDeniedPage() {
  return (
    <main className="access-denied">
      <div className="access-denied__icon" aria-hidden="true">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M12 2 3 7v6c0 5.2 3.9 9.5 9 10 5.1-.5 9-4.8 9-10V7l-9-5z" />
          <path d="M8 12h8" />
        </svg>
      </div>
      <h1 className="access-denied__title">Acesso negado</h1>
      <p className="access-denied__desc" role="alert">
        Você não tem permissão para acessar este recurso.
      </p>
      <Link className="btn btn--primary" to="/">
        Voltar ao início
      </Link>
    </main>
  )
}