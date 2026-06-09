import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  base: '/app/',
  build: {
    outDir: path.resolve(__dirname, '../src/main/resources/static/app'),
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: {
      '/transaction': { target: 'http://localhost:8080', changeOrigin: true },
      '/transaction-report': { target: 'http://localhost:8080', changeOrigin: true },
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/statement': { target: 'http://localhost:8080', changeOrigin: true },
      '/salary': { target: 'http://localhost:8080', changeOrigin: true },
      '/expense': { target: 'http://localhost:8080', changeOrigin: true },
      '/house-rent': { target: 'http://localhost:8080', changeOrigin: true },
      '/endowment': { target: 'http://localhost:8080', changeOrigin: true },
      '/accumulation': { target: 'http://localhost:8080', changeOrigin: true },
      '/medical': { target: 'http://localhost:8080', changeOrigin: true },
      '/unemployment': { target: 'http://localhost:8080', changeOrigin: true },
      '/authentication': { target: 'http://localhost:8080', changeOrigin: true },
      '/logout': { target: 'http://localhost:8080', changeOrigin: true },
      '/login-error.json': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
