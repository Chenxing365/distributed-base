package com.distributed.base.service;

import com.distributed.base.entity.SystemLog;
import com.distributed.base.repository.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 系统业务 Service 层
 *
 * <p>职责：
 * <ul>
 *   <li>MongoDB CRUD（SystemLog 文档）</li>
 *   <li>Redis 缓存读写与原子计数器操作</li>
 *   <li>统计信息汇总（供 REST Controller 与 WebService 共享）</li>
 * </ul>
 *
 * <p>架构设计：此 Service 被 {@code ApiController}（REST）与
 * {@code SystemIntegrationWsImpl}（SOAP）同时注入，
 * 确保两通道的业务逻辑唯一入口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemService {

    /* ─────── 依赖注入 ──────────────────────────────────────── */
    private final SystemLogRepository logRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    /* ─────── Redis Key 常量 ────────────────────────────────── */
    private static final String KEY_LOG_CACHE  = "system:log:cache:";
    private static final String KEY_COUNTER    = "system:api:counter";
    private static final String KEY_STATS      = "system:stats:snapshot";
    private static final long   CACHE_TTL_SEC  = 300; // 5 分钟

    // ================================================================
    // ── MongoDB 操作 ─────────────────────────────────────────────────
    // ================================================================

    /**
     * 写入一条系统日志并返回生成的 ID
     */
    public String saveLog(String module, String action, String operator,
                          String content, String clientIp) {
        SystemLog log = SystemLog.builder()
                .traceId(UUID.randomUUID().toString().replace("-", ""))
                .module(module)
                .action(action)
                .operator(operator)
                .content(content)
                .result("SUCCESS")
                .elapsedMs(0L)
                .clientIp(clientIp)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        SystemLog saved = logRepository.save(log);
        log.info("[MongoDB] 日志写入成功, id={}, module={}", saved.getId(), module);

        // 使缓存失效（下次读取时重新加载）
        invalidateCache(module);
        return saved.getId();
    }

    /**
     * 分页查询日志（按模块过滤）
     */
    public Page<SystemLog> queryLogs(String module, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return logRepository.findByModuleOrderByCreatedAtDesc(module, pageRequest);
    }

    /**
     * 按 ID 查询单条日志
     */
    public Optional<SystemLog> findById(String id) {
        return logRepository.findById(id);
    }

    /**
     * 查询指定模块最近 20 条
     */
    public List<SystemLog> findRecent20(String module) {
        return logRepository.findTop20ByModuleAndActionOrderByCreatedAtDesc(module, "CREATE");
    }

    /**
     * 按模块查询所有（不分页，慎用于大数据量）
     */
    public List<SystemLog> findByModule(String module) {
        return logRepository.findByModuleOrderByCreatedAtDesc(
                module, PageRequest.of(0, 100)).getContent();
    }

    /**
     * 查询 MongoDB 总文档数
     */
    public long countAll() {
        return logRepository.count();
    }

    // ================================================================
    // ── Redis 缓存操作 ──────────────────────────────────────────────
    // ================================================================

    /**
     * 写入缓存（任意对象，JSON 序列化，TTL = 5 分钟）
     */
    public void setCache(String key, Object value) {
        redisTemplate.opsForValue().set(KEY_LOG_CACHE + key, value, CACHE_TTL_SEC, TimeUnit.SECONDS);
        log.debug("[Redis] 缓存写入: key={}, ttl={}s", key, CACHE_TTL_SEC);
    }

    /**
     * 读取缓存
     */
    public Object getCache(String key) {
        Object val = redisTemplate.opsForValue().get(KEY_LOG_CACHE + key);
        log.debug("[Redis] 缓存读取: key={}, hit={}", key, val != null);
        return val;
    }

    /**
     * 删除单个缓存 Key
     */
    public void deleteCache(String key) {
        redisTemplate.delete(KEY_LOG_CACHE + key);
    }

    /**
     * 根据模块使对应缓存失效
     */
    private void invalidateCache(String module) {
        deleteCache(module);
    }

    // ================================================================
    // ── Redis 原子计数器（高并发计数）──────────────────────────────
    // ================================================================

    /**
     * API 调用计数器自增（原子操作，线程安全）
     *
     * @return 自增后的值
     */
    public Long incrementCounter() {
        Long count = stringRedisTemplate.opsForValue().increment(KEY_COUNTER);
        log.debug("[Redis] 计数器自增: key={}, value={}", KEY_COUNTER, count);
        return count;
    }

    /**
     * 获取当前计数器值
     */
    public Long getCounter() {
        String val = stringRedisTemplate.opsForValue().get(KEY_COUNTER);
        return val == null ? 0L : Long.parseLong(val);
    }

    /**
     * 重置计数器
     */
    public void resetCounter() {
        stringRedisTemplate.opsForValue().set(KEY_COUNTER, "0");
        log.info("[Redis] 计数器已重置");
    }

    // ================================================================
    // ── 统计快照（供 SOAP getSystemStats 使用）─────────────────────
    // ================================================================

    /**
     * 获取系统统计快照（先查 Redis 缓存，未命中则实时计算）
     */
    public String getStatsJson() {
        Object cached = redisTemplate.opsForValue().get(KEY_STATS);
        if (cached != null) {
            return cached.toString();
        }

        long mongoTotal   = logRepository.count();
        long successCount = logRepository.countByResult("SUCCESS");
        long failCount    = logRepository.countByResult("FAILURE");
        long apiCounter   = getCounter();

        String json = String.format(
                "{\"mongoTotal\":%d,\"successLogs\":%d,\"failLogs\":%d,\"apiCallCount\":%d,\"timestamp\":\"%s\"}",
                mongoTotal, successCount, failCount, apiCounter, LocalDateTime.now()
        );

        // 缓存 60 秒
        redisTemplate.opsForValue().set(KEY_STATS, json, 60, TimeUnit.SECONDS);
        return json;
    }
}
