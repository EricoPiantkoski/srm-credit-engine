import { expect, test } from '@playwright/test'

import { login } from './helpers'

test('lists receivables and creates a new one', async ({ page }) => {
  await login(page)
  await page.goto('/recebiveis')

  await expect(page.getByText('REC-001')).toBeVisible()

  await page.getByLabel('Referência externa').fill('REC-999')
  await page.getByLabel('Tipo').selectOption('CHEQUE_PRE_DATADO')
  await page.getByLabel('Valor de face').fill('500')
  await page.getByLabel('Moeda').selectOption('USD')
  await page.getByLabel('Vencimento').fill('2026-09-01')
  await page.getByLabel('Cedente').fill('Fornecedor C')
  await page.getByRole('button', { name: 'Cadastrar' }).click()

  await expect(page.getByText('Recebível REC-999 criado.')).toBeVisible()
})