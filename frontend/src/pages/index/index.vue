<template>
  <view class="dashboard">

    <!-- ── 顶部 Header ──────────────────────────────────────── -->
    <view class="header">
      <view class="header-inner">
        <view class="logo-badge">
          <text class="logo-icon">⚡</text>
        </view>
        <view class="header-text">
          <text class="title">分布式基础工程</text>
          <text class="subtitle">Distributed Base Scaffold · 联调看板</text>
        </view>
        <view class="status-dot" :class="serverOnline ? 'online' : 'offline'">
          <text class="status-label">{{ serverOnline ? 'ONLINE' : 'OFFLINE' }}</text>
        </view>
      </view>
    </view>

    <!-- ── 主内容区 ─────────────────────────────────────────── -->
    <scroll-view class="content" scroll-y>

      <!-- 卡片 1：服务健康探针 -->
      <view class="card">
        <view class="card-header">
          <text class="card-icon">🏓</text>
          <text class="card-title">服务健康探针</text>
          <text class="card-tag">REST GET /api/ping</text>
        </view>
        <text class="card-desc">验证后端服务可达性，测试前后端网络连通状态</text>
        <button class="btn btn-primary" :loading="loading.ping" @tap="handlePing">
          发送 Ping
        </button>
        <view class="result-box" v-if="results.ping">
          <text class="result-label">响应结果</text>
          <text class="result-text">{{ results.ping }}</text>
        </view>
      </view>

      <!-- 卡片 2：MongoDB 写入 -->
      <view class="card">
        <view class="card-header">
          <text class="card-icon">🍃</text>
          <text class="card-title">MongoDB 写入日志</text>
          <text class="card-tag">REST POST /api/mongo/log</text>
        </view>
        <text class="card-desc">向 system_logs 集合写入一条文档，返回 ObjectId</text>
        <view class="form-row">
          <input class="input" v-model="form.module"   placeholder="模块名（如 TEST）" />
          <input class="input" v-model="form.operator" placeholder="操作人（如 dev-user）" />
        </view>
        <input class="input input-full" v-model="form.content" placeholder="内容摘要（如 联调测试）" />
        <button class="btn btn-mongo" :loading="loading.mongo" @tap="handleSaveLog">
          写入 MongoDB
        </button>
        <view class="result-box" v-if="results.mongo">
          <text class="result-label">新建文档 ID</text>
          <text class="result-text mono">{{ results.mongo }}</text>
        </view>
      </view>

      <!-- 卡片 3：MongoDB 查询 -->
      <view class="card">
        <view class="card-header">
          <text class="card-icon">🔍</text>
          <text class="card-title">MongoDB 查询日志</text>
          <text class="card-tag">REST GET /api/mongo/log</text>
        </view>
        <text class="card-desc">分页查询指定模块的日志列表</text>
        <view class="form-row">
          <input class="input" v-model="queryModule" placeholder="模块名（默认 TEST）" />
          <button class="btn btn-mongo btn-sm" :loading="loading.mongoQuery" @tap="handleQueryLogs">
            查询
          </button>
        </view>
        <view class="log-list" v-if="logList.length > 0">
          <view class="log-item" v-for="log in logList" :key="log.id">
            <view class="log-meta">
              <text class="log-badge" :class="`badge-${log.result?.toLowerCase()}`">
                {{ log.result }}
              </text>
              <text class="log-module">{{ log.module }}</text>
              <text class="log-action">{{ log.action }}</text>
            </view>
            <text class="log-content">{{ log.content }}</text>
            <text class="log-time">{{ log.createdAt }}</text>
            <text class="log-id mono">ID: {{ log.id }}</text>
          </view>
        </view>
        <view class="empty-tip" v-else-if="queried && logList.length === 0">
          <text>暂无数据，请先写入日志</text>
        </view>
      </view>

      <!-- 卡片 4：Redis 缓存 -->
      <view class="card">
        <view class="card-header">
          <text class="card-icon">⚡</text>
          <text class="card-title">Redis 缓存读写</text>
          <text class="card-tag">REST PUT/GET /api/redis/cache</text>
        </view>
        <text class="card-desc">写入任意键值对到 Redis，TTL=5min；再读取验证命中</text>
        <view class="form-row">
          <input class="input" v-model="cache.key"   placeholder="Key（如 testKey）" />
          <input class="input" v-model="cache.value" placeholder="Value（如 hello world）" />
        </view>
        <view class="btn-group">
          <button class="btn btn-redis" :loading="loading.cacheSet" @tap="handleSetCache">
            写入缓存
          </button>
          <button class="btn btn-redis btn-outline" :loading="loading.cacheGet" @tap="handleGetCache">
            读取缓存
          </button>
        </view>
        <view class="result-box" v-if="results.redis">
          <text class="result-label">Redis 响应</text>
          <text class="result-text">{{ results.redis }}</text>
        </view>
      </view>

      <!-- 卡片 5：Redis 计数器 -->
      <view class="card">
        <view class="card-header">
          <text class="card-icon">🔢</text>
          <text class="card-title">Redis 原子计数器</text>
          <text class="card-tag">REST POST /api/redis/counter/incr</text>
        </view>
        <text class="card-desc">INCR 原子操作演示，线程安全，适合高并发 API 调用计数</text>
        <view class="counter-display">
          <text class="counter-value">{{ counterValue }}</text>
          <text class="counter-label">当前计数</text>
        </view>
        <view class="btn-group">
          <button class="btn btn-accent" :loading="loading.incr" @tap="handleIncrement">
            ＋1 自增
          </button>
          <button class="btn btn-accent btn-outline" :loading="loading.counterGet" @tap="handleGetCounter">
            刷新计数
          </button>
          <button class="btn btn-danger btn-outline" :loading="loading.reset" @tap="handleReset">
            重置为 0
          </button>
        </view>
      </view>

      <!-- 卡片 6：系统统计快照 -->
      <view class="card">
        <view class="card-header">
          <text class="card-icon">📊</text>
          <text class="card-title">系统统计快照</text>
          <text class="card-tag">REST GET /api/stats</text>
        </view>
        <text class="card-desc">汇总 MongoDB 文档总数与 Redis 计数器，60s Redis 缓存</text>
        <button class="btn btn-primary" :loading="loading.stats" @tap="handleGetStats">
          获取统计快照
        </button>
        <view class="stats-grid" v-if="stats">
          <view class="stat-item">
            <text class="stat-value">{{ stats.mongoTotal ?? '—' }}</text>
            <text class="stat-label">MongoDB 总文档</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ stats.successLogs ?? '—' }}</text>
            <text class="stat-label">成功日志</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ stats.failLogs ?? '—' }}</text>
            <text class="stat-label">失败日志</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ stats.apiCallCount ?? '—' }}</text>
            <text class="stat-label">API 调用次数</text>
          </view>
        </view>
      </view>

      <!-- 卡片 7：WebService 说明 -->
      <view class="card card-ws">
        <view class="card-header">
          <text class="card-icon">📡</text>
          <text class="card-title">WebService (SOAP) 说明</text>
          <text class="card-tag">Apache CXF · JAX-WS</text>
        </view>
        <text class="card-desc">
          SOAP 接口与 REST 接口共享同一 Service 层，数据完全一致。
          小程序端无法直接调用 SOAP，建议通过后端 REST 包装或使用 SoapUI 工具测试。
        </text>
        <view class="ws-info-box">
          <view class="ws-row">
            <text class="ws-label">WSDL 地址</text>
            <text class="ws-value mono">http://localhost:8080/ws/SystemIntegrationService?wsdl</text>
          </view>
          <view class="ws-row">
            <text class="ws-label">可用操作</text>
            <text class="ws-value">ping · writeLog · queryRecentLogs · getSystemStats</text>
          </view>
          <view class="ws-row">
            <text class="ws-label">推荐工具</text>
            <text class="ws-value">SoapUI / Postman / Apifox（XML Body）</text>
          </view>
        </view>
        <view class="soap-example">
          <text class="result-label">SOAP Envelope 示例（ping 调用）</text>
          <text class="result-text mono soap-code">{{ soapPingExample }}</text>
        </view>
      </view>

    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  ping,
  saveLog,
  queryLogs,
  setCache,
  getCache,
  incrementCounter,
  getCounter,
  resetCounter,
  getStats,
  type SystemLog
} from '@/api/system'

// ── 服务器在线状态 ───────────────────────────────────────────────
const serverOnline = ref(false)

// ── Loading 状态集合 ─────────────────────────────────────────────
const loading = reactive({
  ping:       false,
  mongo:      false,
  mongoQuery: false,
  cacheSet:   false,
  cacheGet:   false,
  incr:       false,
  counterGet: false,
  reset:      false,
  stats:      false
})

// ── 结果文本集合 ─────────────────────────────────────────────────
const results = reactive({
  ping:  '',
  mongo: '',
  redis: ''
})

// ── 表单数据 ─────────────────────────────────────────────────────
const form = reactive({
  module:   'TEST',
  action:   'CREATE',
  operator: 'dev-user',
  content:  '前端联调测试写入 ' + new Date().toLocaleTimeString()
})

// ── 缓存表单 ─────────────────────────────────────────────────────
const cache = reactive({ key: 'testKey', value: 'hello-from-uniapp' })

// ── 查询状态 ─────────────────────────────────────────────────────
const queryModule = ref('TEST')
const logList     = ref<SystemLog[]>([])
const queried     = ref(false)

// ── 计数器 ───────────────────────────────────────────────────────
const counterValue = ref<number | string>('—')

// ── 统计快照 ─────────────────────────────────────────────────────
const stats = ref<Record<string, number> | null>(null)

// ── WebService SOAP 示例文本 ─────────────────────────────────────
const soapPingExample = `<soapenv:Envelope
  xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
  xmlns:web="http://webservice.base.distributed.com/">
  <soapenv:Header/>
  <soapenv:Body>
    <web:ping/>
  </soapenv:Body>
</soapenv:Envelope>`

// ── 初始化：自动 ping 检测服务器 ────────────────────────────────
onMounted(async () => {
  try {
    const res = await ping()
    serverOnline.value = res.status === 'UP'
    results.ping = JSON.stringify(res, null, 2)
  } catch {
    serverOnline.value = false
  }
  // 初始化加载计数器
  await handleGetCounter()
})

// ── 事件处理 ─────────────────────────────────────────────────────

async function handlePing() {
  loading.ping = true
  try {
    const res = await ping()
    serverOnline.value = res.status === 'UP'
    results.ping = JSON.stringify(res, null, 2)
    uni.showToast({ title: '服务正常 ✓', icon: 'success', duration: 1500 })
  } catch (e) {
    results.ping = `错误: ${(e as Error).message}`
    serverOnline.value = false
  } finally {
    loading.ping = false
  }
}

async function handleSaveLog() {
  if (!form.content.trim()) {
    uni.showToast({ title: '请填写内容摘要', icon: 'none' })
    return
  }
  loading.mongo = true
  try {
    const res = await saveLog({
      module:   form.module,
      action:   form.action,
      operator: form.operator,
      content:  form.content
    })
    results.mongo = res.id ?? '写入成功'
    uni.showToast({ title: '写入成功 ✓', icon: 'success', duration: 1500 })
  } catch (e) {
    results.mongo = `错误: ${(e as Error).message}`
  } finally {
    loading.mongo = false
  }
}

async function handleQueryLogs() {
  loading.mongoQuery = true
  queried.value = false
  try {
    const res = await queryLogs(queryModule.value || 'TEST', 0, 10)
    logList.value = res.data ?? []
    queried.value = true
    if (logList.value.length === 0) {
      uni.showToast({ title: '暂无数据', icon: 'none' })
    }
  } catch (e) {
    uni.showToast({ title: `查询失败: ${(e as Error).message}`, icon: 'none' })
  } finally {
    loading.mongoQuery = false
  }
}

async function handleSetCache() {
  if (!cache.key.trim()) {
    uni.showToast({ title: '请填写 Key', icon: 'none' })
    return
  }
  loading.cacheSet = true
  try {
    const res = await setCache(cache.key, cache.value)
    results.redis = `写入成功: ${JSON.stringify(res)}`
    uni.showToast({ title: '缓存写入成功 ✓', icon: 'success', duration: 1500 })
  } catch (e) {
    results.redis = `错误: ${(e as Error).message}`
  } finally {
    loading.cacheSet = false
  }
}

async function handleGetCache() {
  if (!cache.key.trim()) {
    uni.showToast({ title: '请填写 Key', icon: 'none' })
    return
  }
  loading.cacheGet = true
  try {
    const res = await getCache(cache.key)
    results.redis = `命中: ${res.hit} | Value: ${JSON.stringify(res.value)}`
  } catch (e) {
    results.redis = `错误: ${(e as Error).message}`
  } finally {
    loading.cacheGet = false
  }
}

async function handleIncrement() {
  loading.incr = true
  try {
    const res = await incrementCounter()
    counterValue.value = res.counter ?? 0
    uni.showToast({ title: `计数器: ${res.counter}`, icon: 'none', duration: 1000 })
  } catch (e) {
    uni.showToast({ title: `失败: ${(e as Error).message}`, icon: 'none' })
  } finally {
    loading.incr = false
  }
}

async function handleGetCounter() {
  loading.counterGet = true
  try {
    const res = await getCounter()
    counterValue.value = res.counter ?? 0
  } catch {
    counterValue.value = '—'
  } finally {
    loading.counterGet = false
  }
}

async function handleReset() {
  loading.reset = true
  try {
    await resetCounter()
    counterValue.value = 0
    uni.showToast({ title: '计数器已重置 ✓', icon: 'success', duration: 1500 })
  } catch (e) {
    uni.showToast({ title: `失败: ${(e as Error).message}`, icon: 'none' })
  } finally {
    loading.reset = false
  }
}

async function handleGetStats() {
  loading.stats = true
  try {
    const res = await getStats()
    if (res.stats) {
      stats.value = JSON.parse(res.stats as string)
    }
  } catch (e) {
    uni.showToast({ title: `失败: ${(e as Error).message}`, icon: 'none' })
  } finally {
    loading.stats = false
  }
}
</script>

<style scoped>
/* ── 全局变量 ─────────────────────────────────────────────── */
.dashboard {
  min-height: 100vh;
  background: #0f1117;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

/* ── Header ───────────────────────────────────────────────── */
.header {
  background: linear-gradient(135deg, #1a1d2e 0%, #16213e 50%, #0d1b3e 100%);
  padding: 48rpx 32rpx 32rpx;
  border-bottom: 1rpx solid rgba(99, 179, 237, 0.15);
}
.header-inner {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.logo-badge {
  width: 72rpx;
  height: 72rpx;
  background: linear-gradient(135deg, #4f9cf9, #a855f7);
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-icon { font-size: 40rpx; }
.header-text { flex: 1; }
.title {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
  color: #e2e8f0;
  letter-spacing: 1rpx;
}
.subtitle {
  display: block;
  font-size: 22rpx;
  color: #64748b;
  margin-top: 4rpx;
}
.status-dot {
  padding: 8rpx 18rpx;
  border-radius: 100rpx;
  font-size: 20rpx;
  font-weight: 600;
  letter-spacing: 1rpx;
}
.status-dot.online  { background: rgba(74,222,128,0.15); color: #4ade80; border: 1rpx solid rgba(74,222,128,0.3); }
.status-dot.offline { background: rgba(248,113,113,0.15); color: #f87171; border: 1rpx solid rgba(248,113,113,0.3); }
.status-label { font-size: 20rpx; }

/* ── 内容区 ───────────────────────────────────────────────── */
.content { height: calc(100vh - 160rpx); }

/* ── 卡片 ─────────────────────────────────────────────────── */
.card {
  margin: 24rpx 24rpx 0;
  background: #1a1d2e;
  border: 1rpx solid rgba(99,179,237,0.1);
  border-radius: 24rpx;
  padding: 32rpx;
  transition: border-color 0.2s;
}
.card:last-child { margin-bottom: 40rpx; }
.card-ws { border-color: rgba(168,85,247,0.2); }

.card-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
  flex-wrap: wrap;
}
.card-icon  { font-size: 36rpx; }
.card-title { font-size: 30rpx; font-weight: 600; color: #e2e8f0; flex: 1; }
.card-tag   {
  font-size: 20rpx;
  color: #4f9cf9;
  background: rgba(79,156,249,0.1);
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  font-family: monospace;
}
.card-desc  { display: block; font-size: 24rpx; color: #64748b; margin-bottom: 24rpx; line-height: 1.6; }

/* ── 表单 ─────────────────────────────────────────────────── */
.form-row { display: flex; gap: 16rpx; margin-bottom: 16rpx; }
.input {
  flex: 1;
  background: #0f1117;
  border: 1rpx solid rgba(99,179,237,0.15);
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  font-size: 26rpx;
  color: #e2e8f0;
  margin-bottom: 16rpx;
}
.input-full { width: 100%; box-sizing: border-box; }
.input:focus { border-color: #4f9cf9; }

/* ── 按钮 ─────────────────────────────────────────────────── */
.btn {
  border-radius: 14rpx;
  padding: 20rpx 0;
  font-size: 28rpx;
  font-weight: 600;
  text-align: center;
  width: 100%;
  margin-top: 8rpx;
  letter-spacing: 1rpx;
  transition: opacity 0.15s;
}
.btn-group { display: flex; gap: 16rpx; margin-top: 0; }
.btn-group .btn { flex: 1; margin-top: 0; }
.btn-sm { padding: 20rpx 32rpx; width: auto; flex: 0 0 auto; margin-top: 0; margin-bottom: 0; }

.btn-primary { background: linear-gradient(135deg, #4f9cf9, #3b82f6); color: #fff; border: none; }
.btn-mongo   { background: linear-gradient(135deg, #22c55e, #16a34a); color: #fff; border: none; }
.btn-redis   { background: linear-gradient(135deg, #f59e0b, #d97706); color: #fff; border: none; }
.btn-accent  { background: linear-gradient(135deg, #a855f7, #7c3aed); color: #fff; border: none; }
.btn-danger  { color: #f87171; border: 1rpx solid rgba(248,113,113,0.3); background: rgba(248,113,113,0.05); }
.btn-outline { background: transparent !important; border: 1rpx solid currentColor !important; }
.btn-outline.btn-redis  { color: #f59e0b; }
.btn-outline.btn-accent { color: #a855f7; }

/* ── 结果框 ───────────────────────────────────────────────── */
.result-box {
  margin-top: 20rpx;
  background: #0f1117;
  border: 1rpx solid rgba(99,179,237,0.15);
  border-radius: 12rpx;
  padding: 20rpx;
}
.result-label { display: block; font-size: 20rpx; color: #64748b; margin-bottom: 8rpx; }
.result-text  { display: block; font-size: 24rpx; color: #93c5fd; line-height: 1.6; word-break: break-all; }
.mono { font-family: 'Courier New', Courier, monospace; }

/* ── 日志列表 ─────────────────────────────────────────────── */
.log-list  { margin-top: 16rpx; display: flex; flex-direction: column; gap: 12rpx; }
.log-item  {
  background: #0f1117;
  border: 1rpx solid rgba(99,179,237,0.1);
  border-radius: 12rpx;
  padding: 20rpx;
}
.log-meta    { display: flex; align-items: center; gap: 12rpx; margin-bottom: 8rpx; flex-wrap: wrap; }
.log-badge   { font-size: 20rpx; font-weight: 600; padding: 4rpx 12rpx; border-radius: 8rpx; }
.badge-success { background: rgba(74,222,128,0.15); color: #4ade80; }
.badge-failure { background: rgba(248,113,113,0.15); color: #f87171; }
.badge-unknown { background: rgba(100,116,139,0.2); color: #94a3b8; }
.log-module  { font-size: 22rpx; color: #4f9cf9; font-weight: 600; }
.log-action  { font-size: 22rpx; color: #94a3b8; }
.log-content { display: block; font-size: 24rpx; color: #cbd5e1; margin-bottom: 8rpx; }
.log-time    { display: block; font-size: 20rpx; color: #475569; }
.log-id      { display: block; font-size: 18rpx; color: #334155; margin-top: 4rpx; }
.empty-tip   { text-align: center; color: #475569; font-size: 26rpx; padding: 40rpx 0; }

/* ── 计数器 ───────────────────────────────────────────────── */
.counter-display {
  text-align: center;
  padding: 32rpx 0;
  background: linear-gradient(135deg, rgba(168,85,247,0.08), rgba(79,156,249,0.08));
  border-radius: 16rpx;
  margin-bottom: 20rpx;
}
.counter-value { display: block; font-size: 96rpx; font-weight: 800; color: #a855f7; line-height: 1; }
.counter-label { display: block; font-size: 24rpx; color: #64748b; margin-top: 8rpx; }

/* ── 统计网格 ─────────────────────────────────────────────── */
.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-top: 20rpx;
}
.stat-item {
  background: #0f1117;
  border: 1rpx solid rgba(99,179,237,0.1);
  border-radius: 14rpx;
  padding: 24rpx;
  text-align: center;
}
.stat-value { display: block; font-size: 48rpx; font-weight: 700; color: #4f9cf9; }
.stat-label { display: block; font-size: 22rpx; color: #64748b; margin-top: 8rpx; }

/* ── WebService 说明 ──────────────────────────────────────── */
.ws-info-box {
  background: rgba(168,85,247,0.05);
  border: 1rpx solid rgba(168,85,247,0.15);
  border-radius: 12rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}
.ws-row   { display: flex; gap: 16rpx; margin-bottom: 16rpx; flex-wrap: wrap; }
.ws-row:last-child { margin-bottom: 0; }
.ws-label { font-size: 22rpx; color: #a855f7; font-weight: 600; min-width: 120rpx; }
.ws-value { font-size: 22rpx; color: #94a3b8; flex: 1; word-break: break-all; }
.soap-example { margin-top: 4rpx; }
.soap-code { white-space: pre; font-size: 20rpx; color: #64748b; line-height: 1.8; }
</style>
