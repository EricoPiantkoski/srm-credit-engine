import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'

import { loginRequest } from '../features/auth/api'
import { useTheme } from '../lib/useTheme'

const loginSchema = z.object({
  username: z.string().min(1, 'Usuário é obrigatório'),
  password: z.string().min(1, 'Senha é obrigatória'),
})

type LoginForm = z.infer<typeof loginSchema>

export default function LoginPage() {
  const navigate = useNavigate()
  const theme = useTheme()
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({ resolver: zodResolver(loginSchema) })

  async function onSubmit(values: LoginForm) {
    setError(null)
    try {
      await loginRequest(values.username, values.password)
      navigate('/', { replace: true })
    } catch {
      setError('Usuário ou senha inválidos.')
    }
  }

  const logoSrc = theme.resolved === 'dark' ? '/srm_logo_darkmode.png' : '/srm_logo.png'
  const themeLabel = theme.resolved === 'dark' ? 'Modo claro' : 'Modo escuro'

  return (
    <main className="login">
      <button className="theme-toggle theme-toggle--floating" type="button" onClick={theme.toggle} data-tooltip={themeLabel} aria-label={themeLabel}>
        {theme.resolved === 'dark' ? (
          <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M12 3v2 M12 19v2 M5.64 5.64l1.41 1.41 M16.95 16.95l1.41 1.41 M3 12h2 M19 12h2 M5.64 18.36l1.41-1.41 M16.95 7.05l1.41-1.41 M8 12a4 4 0 1 1 8 0 4 4 0 0 1-8 0" />
          </svg>
        ) : (
          <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
          </svg>
        )}
      </button>
      <div className="login__card">
        <div className="login__brand">
          <img className="login__logo" src={logoSrc} alt="SRM Credit Engine" />
        </div>

        <h1 className="login__title">Acessar o sistema</h1>
        <p className="login__subtitle">Entre com suas credenciais de administrador</p>

        <form className="form" onSubmit={handleSubmit(onSubmit)} noValidate>
          <div className="form__group">
            <label className="label" htmlFor="username">
              Usuário
            </label>
            <input
              id="username"
              className="input"
              autoComplete="username"
              placeholder="admin"
              {...register('username')}
            />
            {errors.username && <p className="field-error" role="alert">{errors.username.message}</p>}
          </div>

          <div className="form__group">
            <label className="label" htmlFor="password">
              Senha
            </label>
            <input
              id="password"
              className="input"
              type="password"
              autoComplete="current-password"
              placeholder="••••••••"
              {...register('password')}
            />
            {errors.password && <p className="field-error" role="alert">{errors.password.message}</p>}
          </div>

          {error && (
            <p className="alert alert--error" role="alert">
              {error}
            </p>
          )}

          <button className="btn btn--primary btn--block" type="submit" data-tooltip="Entrar no sistema" disabled={isSubmitting}>
            Entrar
          </button>
        </form>
      </div>
    </main>
  )
}
