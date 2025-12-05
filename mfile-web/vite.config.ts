import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // 🟢 把所有 /api 开头的请求转发给 Spring Boot
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // rewrite: (path) => path.replace(/^\/api/, '') // 如果后端不需要 /api 前缀，就把这行解开
      }
    }
  }
})