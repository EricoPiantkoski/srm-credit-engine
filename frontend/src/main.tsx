import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import * as Sentry from '@sentry/react'
import App from './app/App'
import { initTheme } from './lib/theme'
import './styles/global.css'

const sentryDsn = import.meta.env.VITE_SENTRY_DSN

if (sentryDsn) {
  Sentry.init({
    dsn: sentryDsn,
    environment: import.meta.env.VITE_SENTRY_ENVIRONMENT ?? 'development',
    integrations: [Sentry.browserTracingIntegration(), Sentry.replayIntegration()],
    tracesSampleRate: 0.1,
    replaysSessionSampleRate: 0.1,
    replaysOnErrorSampleRate: 1,
  })
}

initTheme()

async function cleanupStaleServiceWorkers() {
  if ('serviceWorker' in navigator) {
    const registrations = await navigator.serviceWorker.getRegistrations()
    await Promise.all(registrations.map((registration) => registration.unregister()))
  }
}

async function enableMocking() {
  if (import.meta.env.VITE_MSW_ENABLED !== 'true') {
    await cleanupStaleServiceWorkers()
    return
  }
  const { worker } = await import('./test/browser')
  await worker.start({ onUnhandledRequest: 'error' })
}

enableMocking().then(() => {
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <Sentry.ErrorBoundary fallback={<p role="alert">Não foi possível carregar a aplicação.</p>}>
        <App />
      </Sentry.ErrorBoundary>
    </StrictMode>,
  )
})
