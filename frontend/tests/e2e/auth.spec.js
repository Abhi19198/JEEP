import { expect, test } from '@playwright/test'

test.describe('authentication surface', () => {
  test('shows login and switches to registration', async ({ page }) => {
    await page.goto('/')

    await expect(page.getByRole('heading', { name: 'JEEP' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Login', exact: true }).first()).toBeVisible()
    await expect(page.getByLabel('Email')).toBeVisible()

    await page.getByRole('button', { name: 'Register', exact: true }).click()

    await expect(page.getByLabel('First name')).toBeVisible()
    await expect(page.getByLabel('Last name')).toBeVisible()
    await expect(page.getByLabel('Confirm password')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Create account' })).toBeVisible()
  })

  test('keeps required registration fields enforced', async ({ page }) => {
    await page.goto('/')
    await page.getByRole('button', { name: 'Register', exact: true }).click()

    const email = page.getByLabel('Email')
    await expect(email).toHaveAttribute('required', '')
    await expect(page.getByLabel('Password', { exact: true })).toHaveAttribute('required', '')
    await expect(page.getByLabel('Confirm password')).toHaveAttribute('required', '')
  })
})
