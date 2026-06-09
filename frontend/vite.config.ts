import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

const apiPrefixes = [
  '/transaction', '/transaction-report', '/api', '/statement', '/salary', '/expense',
  '/house-rent', '/endowment', '/accumulation', '/medical', '/unemployment',
  '/authentication', '/logout', '/login-error.json',
]

export default defineConfig({
  plugins: [react()],
  base: '/app/',
  build: {
    outDir: path.resolve(__dirname, '../src/main/resources/static/app'),
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: Object.fromEntries(
      apiPrefixes.map((prefix) => [prefix, { target: 'http://localhost:8080', changeOrigin: true }]),
    ),
  },
})
