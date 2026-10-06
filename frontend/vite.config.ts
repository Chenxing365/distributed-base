import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

/**
 * Vite 配置
 *
 * 构建目标：H5 (Web) + 微信小程序（通过 UNI_PLATFORM 环境变量切换）
 */
export default defineConfig({
  plugins: [uni()],

  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      // 开发时代理后端 API，避免跨域
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api')
      },
      // WebService 代理
      '/ws': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  },

  resolve: {
    alias: {
      '@': '/src'
    }
  }
})
