import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/charge':  { target: 'http://localhost:8081', changeOrigin: true, rewrite: p => p.replace(/^\/api\/charge/, '') },
      '/api/grid':    { target: 'http://localhost:8082', changeOrigin: true, rewrite: p => p.replace(/^\/api\/grid/, '') },
      '/api/billing': { target: 'http://localhost:8083', changeOrigin: true, rewrite: p => p.replace(/^\/api\/billing/, '') },
    }
  }
})
