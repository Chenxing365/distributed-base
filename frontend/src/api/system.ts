/**
 * src/api/system.ts
 *
 * API 调用集中管理层
 *
 * 规范：
 * - 所有后端接口调用必须通过此文件，不允许在页面/组件中直接调用 http.*
 * - 每个函数对应一个后端 REST 端点，保持 1:1 映射
 * - TypeScript 类型与后端 ApiController 返回结构严格对应
 */

import { http } from '@/utils/request'

// ── 类型定义 ────────────────────────────────────────────────────────

/** MongoDB 日志实体（对应后端 SystemLog.java）*/
export interface SystemLog {
  id:        string
  traceId:   string
  module:    string
  action:    string
  operator:  string
  content:   string
  result:    string
  elapsedMs: number
  clientIp:  string
  createdAt: string
  updatedAt: string
}

/** 写入日志请求体 */
export interface SaveLogRequest {
  module:   string
  action:   string
  operator: string
  content:  string
}

/** 分页查询结果 */
export interface PageResult<T> {
  data:        T[]
  total:       number
  totalPages:  number
  currentPage: number
}

/** Redis 缓存写入请求体 */
export interface SetCacheRequest {
  key:   string
  value: unknown
}

// ── 健康探针 ────────────────────────────────────────────────────────

/**
 * 服务健康探针
 * @returns { status: 'UP', message: 'pong', timestamp, service }
 */
export const ping = () =>
  http.get<{ status: string; message: string; timestamp: string; service: string }>('/api/ping')

// ── MongoDB 操作 ────────────────────────────────────────────────────

/**
 * 写入一条系统日志到 MongoDB
 */
export const saveLog = (payload: SaveLogRequest) =>
  http.post<{ id: string; elapsedMs: number }>('/api/mongo/log', payload)

/**
 * 分页查询日志列表
 * @param module  模块名（默认 TEST）
 * @param page    页码，0 起始
 * @param size    每页条数
 */
export const queryLogs = (module = 'TEST', page = 0, size = 10) =>
  http.get<PageResult<SystemLog>>('/api/mongo/log', { module, page, size })

/**
 * 按 ID 查询单条日志
 */
export const findLogById = (id: string) =>
  http.get<SystemLog>(`/api/mongo/log/${id}`)

// ── Redis 缓存操作 ──────────────────────────────────────────────────

/**
 * 写入 Redis 缓存（TTL = 5 分钟）
 */
export const setCache = (key: string, value: unknown) =>
  http.put<{ key: string; message: string }>('/api/redis/cache', { key, value })

/**
 * 读取 Redis 缓存
 */
export const getCache = (key: string) =>
  http.get<{ key: string; value: unknown; hit: boolean }>('/api/redis/cache', { key })

// ── Redis 计数器操作 ────────────────────────────────────────────────

/**
 * 计数器原子自增（每次 +1，线程安全）
 */
export const incrementCounter = () =>
  http.post<{ counter: number }>('/api/redis/counter/incr')

/**
 * 读取当前计数器值
 */
export const getCounter = () =>
  http.get<{ counter: number }>('/api/redis/counter')

/**
 * 重置计数器为 0
 */
export const resetCounter = () =>
  http.delete<{ counter: number; message: string }>('/api/redis/counter/reset')

// ── 系统统计 ────────────────────────────────────────────────────────

/**
 * 获取系统统计快照（MongoDB 文档总数 + Redis 计数器，60s 缓存）
 */
export const getStats = () =>
  http.get<{ stats: string; timestamp: string }>('/api/stats')
