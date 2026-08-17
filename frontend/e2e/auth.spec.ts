import { expect, test } from '@playwright/test'

import { login } from './helpers'

test('protected route redirects unauthenticated users to login', async ({ page }) => {
  await page.goto('/cambio')

  await expect(page.getByRole('button', { name: 'Entrar' })).toBeVisible()
})

test('logs in, navigates the panels and logs out', async ({ page }) => {
  await login(page)

  const menu = page.getByRole('navigation', { name: 'Menu principal' })
  await menu.getByRole('link', { name: 'Câmbio' }).click()
  await expect(page.getByRole('heading', { name: 'Operação de Câmbio' })).toBeVisible()

  await menu.getByRole('link', { name: 'Extrato' }).click()
  await expect(page.getByRole('heading', { name: 'Extrato de Liquidações' })).toBeVisible()

  await page.getByRole('button', { name: 'Sair' }).click()
  await expect(page.getByRole('button', { name: 'Entrar' })).toBeVisible()
})