import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    target: 'es2015',
    cssCodeSplit: true,
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      output: {
        manualChunks(id: string) {
          if (id.includes('node_modules')) {
            if (id.includes('vue') || id.includes('vue-router')) return 'vue-vendor'
            if (id.includes('lucide-vue-next')) return 'ui-vendor'
            if (id.includes('chart.js') || id.includes('vue-chartjs')) return 'chart-vendor'
            if (id.includes('@vueup/vue-quill') || id.includes('katex')) return 'editor-vendor'
            return 'vendor'
          }
        }
      }
    } as any
  },
  esbuild: {
    drop: ['console', 'debugger'] as any[]
  } as any,
  server: {
    proxy: {
      // 统一代理所有 /api 开头的请求到后端
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 静态文件上传目录
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
