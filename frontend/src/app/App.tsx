import { useEffect } from 'react'
import { BrowserRouter, Navigate, Route, Routes, useNavigate } from 'react-router-dom'

import { onUnauthorized } from '../features/auth/refresh'
import { DashboardLayout } from '../features/auth/DashboardLayout'
import { ProtectedRoute } from '../features/auth/ProtectedRoute'
import AccessDeniedPage from '../pages/AccessDeniedPage'
import CambioPage from '../pages/CambioPage'
import ExtratoPage from '../pages/ExtratoPage'
import HomePage from '../pages/HomePage'
import LiquidacaoPage from '../pages/LiquidacaoPage'
import LoginPage from '../pages/LoginPage'
import RecebiveisPage from '../pages/RecebiveisPage'
import SimulacaoPage from '../pages/SimulacaoPage'
import { AppProviders } from './providers'

function SessionExpiryRedirect() {
  const navigate = useNavigate()

  useEffect(
    () => onUnauthorized(() => navigate('/login', { replace: true, state: { reason: 'expired' } })),
    [navigate],
  )

  return null
}

export default function App() {
  return (
    <AppProviders>
      <BrowserRouter>
        <SessionExpiryRedirect />
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/access-denied" element={<AccessDeniedPage />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <DashboardLayout>
                  <HomePage />
                </DashboardLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/cambio"
            element={
              <ProtectedRoute>
                <DashboardLayout>
                  <CambioPage />
                </DashboardLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/recebiveis"
            element={
              <ProtectedRoute>
                <DashboardLayout>
                  <RecebiveisPage />
                </DashboardLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/simulacao"
            element={
              <ProtectedRoute>
                <DashboardLayout>
                  <SimulacaoPage />
                </DashboardLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/liquidacoes"
            element={
              <ProtectedRoute>
                <DashboardLayout>
                  <LiquidacaoPage />
                </DashboardLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/extrato"
            element={
              <ProtectedRoute>
                <DashboardLayout>
                  <ExtratoPage />
                </DashboardLayout>
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AppProviders>
  )
}