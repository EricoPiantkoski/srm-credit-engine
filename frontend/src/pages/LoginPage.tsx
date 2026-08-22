import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'

import { loginRequest } from '../features/auth/api'

const loginSchema = z.object({
  username: z.string().min(1, 'Usuário é obrigatório'),
  password: z.string().min(1, 'Senha é obrigatória'),
})

type LoginForm = z.infer<typeof loginSchema>

export default function LoginPage() {
  const navigate = useNavigate()
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

  const logoSrc = '/srm_logo.png'

  return (
    <main className="login">
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
