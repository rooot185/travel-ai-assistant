import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => ({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': loadEnv(mode, '.', 'TRAVEL_DEV_').TRAVEL_DEV_API ?? 'http://127.0.0.1:8080',
    },
  },
}))
