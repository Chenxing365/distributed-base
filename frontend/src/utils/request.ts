/**
 * src/utils/request.ts
 *
 * 统一网络请求封装 —— 抹平 H5（fetch/axios）与微信小程序（wx.request）API 差异
 *
 * 功能：
 * 1. BaseURL 动态注入（H5 走 Vite 代理 / 小程序直连 HTTPS）
 * 2. 请求拦截：自动携带 Authorization Token
 * 3. 响应拦截：统一错误码处理 & 全局错误提示
 * 4. TypeScript 泛型支持，保证调用侧类型安全
 */

// ── 运行平台判断 ──────────────────────────────────────────────────
const IS_H5 = typeof window !== 'undefined' && typeof wx === 'undefined'

// ── BaseURL 配置 ──────────────────────────────────────────────────
/**
 * H5 端：使用相对路径 '' — 由 Vite devServer.proxy 代理到 8080
 *        生产部署时由 Nginx 反向代理，同域名无需填写
 * 小程序端：填写实际后端 HTTPS 地址（必须已备案并配置安全域名）
 */
const BASE_URL_H5      = ''                             // H5 同域代理
const BASE_URL_MP      = 'https://your-server.com'     // 小程序直连地址（按需修改）
const BASE_URL         = IS_H5 ? BASE_URL_H5 : BASE_URL_MP

// ── Token 工具函数 ────────────────────────────────────────────────
function getToken(): string | null {
  try {
    return uni.getStorageSync('__access_token__') || null
  } catch {
    return null
  }
}

function setToken(token: string): void {
  uni.setStorageSync('__access_token__', token)
}

function clearToken(): void {
  uni.removeStorageSync('__access_token__')
}

// ── 请求配置类型 ──────────────────────────────────────────────────
export interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  data?: Record<string, unknown> | unknown
  headers?: Record<string, string>
  timeout?: number        // 超时（毫秒），默认 10000
  showError?: boolean     // 是否展示全局错误提示，默认 true
}

// ── 响应类型 ──────────────────────────────────────────────────────
export interface ApiResponse<T = unknown> {
  success: boolean
  data?: T
  message?: string
  error?: string
  [key: string]: unknown
}

// ── 核心请求函数 ──────────────────────────────────────────────────
/**
 * 统一请求方法
 *
 * @example
 * const res = await request<{ id: string }>({ url: '/api/mongo/log', method: 'POST', data: {...} })
 */
export function request<T = unknown>(options: RequestOptions): Promise<ApiResponse<T>> {
  const {
    url,
    method    = 'GET',
    data,
    headers   = {},
    timeout   = 10000,
    showError = true
  } = options

  // ── 请求拦截：组装完整 URL & Header ──────────────────────────
  const fullUrl     = `${BASE_URL}${url}`
  const token       = getToken()
  const finalHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
    ...headers
  }
  if (token) {
    finalHeaders['Authorization'] = `Bearer ${token}`
  }

  // ── 发送请求（uni.request 兼容所有端）─────────────────────────
  return new Promise((resolve, reject) => {
    uni.request({
      url:     fullUrl,
      method,
      data:    method === 'GET' ? data as Record<string, unknown> : JSON.stringify(data),
      header:  finalHeaders,
      timeout,
      success(res) {
        const statusCode = res.statusCode

        // ── 响应拦截：HTTP 4xx / 5xx 错误处理 ──────────────────
        if (statusCode === 401) {
          clearToken()
          showError && uni.showToast({ title: '登录已过期，请重新登录', icon: 'none', duration: 2000 })
          reject(new Error('Unauthorized'))
          return
        }

        if (statusCode === 403) {
          showError && uni.showToast({ title: '无操作权限', icon: 'none', duration: 2000 })
          reject(new Error('Forbidden'))
          return
        }

        if (statusCode >= 500) {
          showError && uni.showToast({ title: '服务器内部错误，请稍后重试', icon: 'none', duration: 3000 })
          reject(new Error(`Server Error: ${statusCode}`))
          return
        }

        // ── 业务层错误（success: false）─────────────────────────
        const body = res.data as ApiResponse<T>
        if (body && body.success === false && showError) {
          uni.showToast({
            title: body.message || '操作失败',
            icon:  'none',
            duration: 2500
          })
        }

        resolve(body)
      },
      fail(err) {
        // ── 网络错误（超时 / DNS 失败等）─────────────────────────
        console.error('[request] 网络请求失败:', err)
        if (showError) {
          uni.showToast({ title: '网络异常，请检查网络连接', icon: 'none', duration: 3000 })
        }
        reject(new Error(err.errMsg || 'Network Error'))
      }
    })
  })
}

// ── 便捷方法 ──────────────────────────────────────────────────────
export const http = {
  get<T = unknown>(url: string, data?: Record<string, unknown>, opts?: Partial<RequestOptions>) {
    return request<T>({ url, method: 'GET', data, ...opts })
  },
  post<T = unknown>(url: string, data?: unknown, opts?: Partial<RequestOptions>) {
    return request<T>({ url, method: 'POST', data, ...opts })
  },
  put<T = unknown>(url: string, data?: unknown, opts?: Partial<RequestOptions>) {
    return request<T>({ url, method: 'PUT', data, ...opts })
  },
  delete<T = unknown>(url: string, data?: unknown, opts?: Partial<RequestOptions>) {
    return request<T>({ url, method: 'DELETE', data, ...opts })
  }
}

// 导出 Token 工具供登录模块使用
export { setToken, clearToken, getToken }
