import type { Page } from '@playwright/test'

export async function login(page: Page) {
  await page.goto('/login')
  await page.getByLabel('Usuário').fill('admin')
  await page.getByLabel('Senha').fill('admin123')
  await page.getByRole('button', { name: 'Entrar' }).click()
  await page.getByRole('heading', { name: 'SRM Credit Engine' }).waitFor()
}