import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// Dev: `npm run dev` on :5173 proxies /api to Spring Boot on :8080, so the browser still sees one
// origin and session/CSRF cookies behave exactly like production.
// Build: output goes into Spring Boot's static folder and ships inside the jar.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
  },
})
