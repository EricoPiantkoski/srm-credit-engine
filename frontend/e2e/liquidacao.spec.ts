import { expect, test } from '@playwright/test'

import { login } from './helpers'

test('creates a liquidation selecting an available receivable', async ({ page }) => {
  await login(page)
  await page.goto('/liquidacoes')

  await page.getByLabel('Moeda de pagamento').selectOption('BRL')
  await page.getByLabel(/1 — REC-001/).check()
  await page.getByRole('button', { name: 'Liquidar' }).click()

  await expect(page.getByTestId('liquidacao-resultado')).toContainText('LIQUIDADA')
  await expect(page.getByText('Liquidação #1')).toBeVisible()
})