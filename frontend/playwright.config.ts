import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  use: {
    baseURL: 'http://localhost:5173',
  },
  webServer: [
    {
      command: 'docker compose up -d && mvn spring-boot:run',
      url: 'http://localhost:8080/api/personen',
      reuseExistingServer: true,
      timeout: 120_000,
    },
    {
      command: 'cd frontend && yarn dev',
      url: 'http://localhost:5173',
      reuseExistingServer: true,
      timeout: 60_000,
    },
  ],
  projects: [{ name: 'mobil', use: { ...devices['iPhone 13'] } }],
})
