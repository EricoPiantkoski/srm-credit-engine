import { expect, test } from '@playwright/test'

import { login } from './helpers'

test('consults the current exchange rate', async ({ page }) => {
  await login(page)
  await page.goto('/cambio')

  await page.getByRole('button', { name: 'BRL como moeda de origem para consulta' }).click()
  await page.getByRole('button', { name: 'USD como moeda de destino para consulta' }).click()
  await page.getByRole('button', { name: 'Consultar cotação' }).click()

  await expect(page.getByTestId('taxa-resultado')).toContainText('0,18')
})
