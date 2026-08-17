import { useCallback, useEffect, useState } from 'react'

import { getResolvedTheme, toggleTheme, watchSystemTheme } from './theme'

export interface UseThemeResult {
  resolved: 'light' | 'dark'
  toggle: () => void
}

export function useTheme(): UseThemeResult {
  const [resolved, setResolved] = useState<'light' | 'dark'>(getResolvedTheme)

  useEffect(() => {
    const unsubscribe = watchSystemTheme(() => {
      setResolved(getResolvedTheme())
    })
    return unsubscribe
  }, [])

  const toggle = useCallback(() => {
    toggleTheme()
    setResolved(getResolvedTheme())
  }, [])

  return { resolved, toggle }
}
