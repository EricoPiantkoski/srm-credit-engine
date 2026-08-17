export type Theme = 'light' | 'dark' | 'system'

const STORAGE_KEY = 'srm-theme'

function systemPrefersDark(): boolean {
  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-color-scheme: dark)').matches
  )
}

export function readTheme(): Theme {
  if (typeof window === 'undefined') {
    return 'system'
  }
  const value = window.localStorage.getItem(STORAGE_KEY)
  return value === 'light' || value === 'dark' || value === 'system' ? value : 'system'
}

export function getResolvedTheme(): 'light' | 'dark' {
  const theme = readTheme()
  if (theme === 'system') {
    return systemPrefersDark() ? 'dark' : 'light'
  }
  return theme
}

export function setTheme(theme: Theme): void {
  if (typeof document === 'undefined') {
    return
  }
  const root = document.documentElement

  // Persiste antes para getResolvedTheme() ler o novo tema
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(STORAGE_KEY, theme)
  }

  const resolved = getResolvedTheme()
  if (theme === 'system') {
    root.removeAttribute('data-theme')
  } else {
    root.setAttribute('data-theme', theme)
  }
  root.style.colorScheme = resolved

  // Favicon dinâmico baseado no tema
  const favicon = document.querySelector("link[rel~='icon']") as HTMLLinkElement | null
  if (favicon) {
    favicon.href = resolved === 'dark' ? '/srm_logo_2_darkmode.png' : '/srm_logo_2.png'
  }
}

export function initTheme(): void {
  setTheme(readTheme())
}

export function toggleTheme(): Theme {
  const next: Theme = getResolvedTheme() === 'dark' ? 'light' : 'dark'
  setTheme(next)
  return next
}

export function watchSystemTheme(onChange: () => void): () => void {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return () => undefined
  }
  const mq = window.matchMedia('(prefers-color-scheme: dark)')
  const handler = () => {
    if (readTheme() === 'system') {
      setTheme('system')
    }
    onChange()
  }
  mq.addEventListener('change', handler)
  return () => mq.removeEventListener('change', handler)
}
