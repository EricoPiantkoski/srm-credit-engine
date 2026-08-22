import { NavLink, useNavigate } from 'react-router-dom'
import { useState } from 'react'

import { useTheme } from '../../lib/useTheme'
import { logoutRequest } from './api'
import { useSession } from './session'

interface DashboardLayoutProps {
  children: React.ReactNode
}

function Icon({ path }: { path: string; title?: string }) {
  return (
    <svg
      className="icon"
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

const NAV_ITEMS = [
  { to: '/', label: 'Início', path: 'M3 12l9-9 9 9 M5 10v10a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1V10' },
  { to: '/cambio', label: 'Câmbio', path: 'M3 7h13l3 3v7a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V8a1 1 0 0 1 1-1z M3 7V5a1 1 0 0 1 1-1h9l2 3' },
  { to: '/recebiveis', label: 'Recebíveis', path: 'M8 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8z M4 20c0-2.2 1.8-4 4-4s4 1.8 4 4 M16 11h6 M19 8v6' },
  { to: '/simulacao', label: 'Simulação', path: 'M3 3v18h18 M7 15l4-6 4 3 5-7' },
  { to: '/liquidacoes', label: 'Liquidação', path: 'M5 3h14a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z M9 8h6 M9 12h6 M9 16h3' },
  { to: '/extrato', label: 'Extrato', path: 'M6 2h9l5 5v15a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V3a1 1 0 0 1-1-1V3a1 1 0 0 1-1-1z M14 2v6h6 M9 13h6 M9 17h6' },
]

export function DashboardLayout({ children }: DashboardLayoutProps) {
  const navigate = useNavigate()
  const refreshToken = useSession((state) => state.refreshToken)
  const theme = useTheme()
  const [drawerOpen, setDrawerOpen] = useState(false)

  async function handleLogout() {
    if (refreshToken) {
      await logoutRequest(refreshToken)
    }
    navigate('/login', { replace: true })
  }

  const logoSrc = theme.resolved === 'dark' ? '/srm_logo_darkmode.png' : '/srm_logo.png'
  const themeLabel = theme.resolved === 'dark' ? 'Modo claro' : 'Modo escuro'

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar__brand">
          <NavLink to="/" aria-label="Início" style={{ display: 'inline-block', lineHeight: 0 }}>
            <img className="sidebar__logo" src={logoSrc} alt="SRM Credit Engine" />
          </NavLink>
        </div>
        <nav className="sidebar__nav" aria-label="Menu principal">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) => (isActive ? 'nav__link nav__link--active' : 'nav__link')}
              onClick={() => setDrawerOpen(false)}
            >
              <Icon path={item.path} title={item.label} />
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar__footer">
          <div className="sidebar__footer-row">
            <button
              className="sidebar__footer-btn"
              type="button"
              onClick={theme.toggle}
               data-tooltip={themeLabel}
              aria-label={themeLabel}
            >
              {theme.resolved === 'dark' ? (
                <Icon
                  path="M12 3v2 M12 19v2 M5.64 5.64l1.41 1.41 M16.95 16.95l1.41 1.41 M3 12h2 M19 12h2 M5.64 18.36l1.41-1.41 M16.95 7.05l1.41-1.41 M8 12a4 4 0 1 1 8 0 4 4 0 0 1-8 0"
                  title="Modo claro"
                />
              ) : (
                <Icon
                  path="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"
                  title="Modo escuro"
                />
              )}
            </button>
            <button
              className="sidebar__footer-btn sidebar__footer-btn--logout"
              type="button"
              onClick={handleLogout}
               data-tooltip="Sair"
              aria-label="Sair"
            >
              <Icon path="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4 M16 17l5-5-5-5 M21 12H9" title="Sair" />
            </button>
          </div>
        </div>

        <button
          className="hamburger"
          type="button"
          onClick={() => setDrawerOpen(true)}
          aria-label="Abrir menu"
          aria-expanded={drawerOpen}
        >
          <Icon path="M3 12h18 M3 6h18 M3 18h18" title="Menu" />
        </button>

        <div className={`drawer-overlay ${drawerOpen ? 'open' : ''}`} onClick={() => setDrawerOpen(false)} aria-hidden={!drawerOpen}>
          <div className={`drawer ${drawerOpen ? 'open' : ''}`} role="dialog" aria-label="Menu de navegação" onClick={(e) => e.stopPropagation()}>
            <div className="drawer__header">
              <button className="drawer__close" type="button" onClick={() => setDrawerOpen(false)} aria-label="Fechar menu">
                <Icon path="M18 6L6 18 M6 6l12 12" title="Fechar" />
              </button>
            </div>
            <nav className="drawer__nav" aria-label="Menu principal">
              {NAV_ITEMS.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.to === '/'}
                  className={({ isActive }) => (isActive ? 'drawer__link drawer__link--active' : 'drawer__link')}
                  onClick={() => setDrawerOpen(false)}
                >
                  <Icon path={item.path} title={item.label} />
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </div>
        </div>
      </aside>
      <div className="main">{children}</div>
    </div>
  )
}