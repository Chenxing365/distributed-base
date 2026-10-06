import { createSSRApp } from 'vue'
import App from './App.vue'

/**
 * UniApp 应用入口
 * createSSRApp: UniApp 要求使用此方法，兼容 SSR / 小程序环境
 */
export function createApp() {
  const app = createSSRApp(App)
  return { app }
}
