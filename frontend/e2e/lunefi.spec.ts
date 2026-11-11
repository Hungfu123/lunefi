import { expect, test } from '@playwright/test'

test('Zwei Personen erfassen gemeinsame Ausgaben und sehen den Ausgleich', async ({ page }) => {
  await page.goto('/')

  if (await page.locator('text=Willkommen bei LuneFi').isVisible()) {
    await page.locator('#name1').fill('Alex')
    await page.locator('#name2').fill('Kim')
    await page.getByRole('button', { name: "Los geht's" }).click()
    await expect(page.getByText('Ausgaben')).toBeVisible()
  }

  await page.getByLabel('Eintrag erfassen').click()
  await page.locator('input[placeholder="-12,50"]').fill('-60')
  await page.locator('input[placeholder="oder neu eingeben"]').fill('Essen')
  await page.getByRole('switch', { name: 'Gemeinsame Ausgabe' }).click()
  await page.getByRole('button', { name: 'Speichern' }).click()
  await expect(page.getByText('gemeinsam (Alex hat bezahlt)')).toBeVisible()

  await page.getByRole('link', { name: 'Ausgleich' }).click()
  await expect(page.getByText('Alles ausgeglichen')).toBeVisible()
})
