/// <reference types="vitest/config" />
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import { sentryVitePlugin } from '@sentry/vite-plugin'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  const plugins = [react()]
  if (env.SENTRY_AUTH_TOKEN && env.SENTRY_ORG && env.SENTRY_PROJECT) {
    plugins.push(sentryVitePlugin({
      authToken: env.SENTRY_AUTH_TOKEN,
      org: env.SENTRY_ORG,
      project: env.SENTRY_PROJECT,
      telemetry: false,
    }))
  }

  return {
    plugins,
    test: {
      environment: 'jsdom',
      globals: true,
      testTimeout: 15_000,
      setupFiles: ['./src/test/setup.ts'],
      exclude: ['e2e/**', 'node_modules/**', 'dist/**'],
      coverage: {
        provider: 'v8',
        reporter: ['text', 'html', 'json-summary'],
        exclude: [
          '**/node_modules/**',
          '**/dist/**',
          '**/e2e/**',
          '**/public/**',
          '**/*.test.{ts,tsx}',
          'src/app/**',
          'src/main.tsx',
          'src/test/**',
          '**/types.ts',
          '**/*.d.ts',
        ],
        thresholds: {
          lines: 80,
          functions: 70,
          branches: 65,
          statements: 80,
        },
      },
    },
  }
})
