package com.distributed.base.controller;

import com.distributed.base.entity.SystemLog;
import com.distributed.base.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 统一联调接口 Controller
 *
 * <p>提供以下 REST 接口，供前端联调面板调用：
 * <ul>
 *   <li>GET  /api/ping                    — 健康探针</li>
 *   <li>POST /api/mongo/log               — 写入 MongoDB 日志</li>
 *   <li>GET  /api/mongo/log               — 分页查询日志</li>
 *   <li>GET  /api/mongo/log/{id}          — 按 ID 查单条</li>
 *   <li>PUT  /api/redis/cache             — 写入 Redis 缓存</li>
 *   <li>GET  /api/redis/cache             — 读取 Redis 缓存</li>
 *   <li>POST /api/redis/counter/incr      — 计数器自增</li>
 *   <li>GET  /api/redis/counter           — 读取计数器</li>
 *   <li>DELETE /api/redis/counter/reset   — 重置计数器</li>
 *   <li>GET  /api/stats                   — 系统统计快照</li>
 * </ul>
 *
 * <p>Swagger UI: http://localhost:8080/swagger-ui.html
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "系统联调接口", description = "打通 MongoDB / Redis / WebService 的集成测试 API")
public class ApiController {

    private final SystemService systemService;

    // ================================================================
    // ── 健康探针 ─────────────────────────────────────────────────────
    // ================================================================

    @GetMapping("/ping")
    @Operation(summary = "健康探针", description = "验证服务存活，返回 pong 与时间戳")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "UP");
        resp.put("message", "pong");
        resp.put("timestamp", LocalDateTime.now().toString());
        resp.put("service", "distributed-base");
        return ResponseEntity.ok(resp);
    }

    // ================================================================
    // ── MongoDB：日志写入 & 查询 ─────────────────────────────────────
    // ================================================================

    /**
     * 写入一条系统日志到 MongoDB
     *
     * <p>请求体示例：
     * <pre>{
     *   "module":   "TEST",
     *   "action":   "CREATE",
     *   "operator": "dev-user",
     *   "content":  "联调测试写入"
     * }</pre>
     */
    @PostMapping("/mongo/log")
    @Operation(summary = "写入日志", description = "向 MongoDB system_logs 集合写入一条日志记录")
    public ResponseEntity<Map<String, Object>> saveLog(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String module   = body.getOrDefault("module",   "TEST");
        String action   = body.getOrDefault("action",   "CREATE");
        String operator = body.getOrDefault("operator", "anonymous");
        String content  = body.getOrDefault("content",  "");
        String clientIp = request.getRemoteAddr();

        long start = System.currentTimeMillis();
        String id = systemService.saveLog(module, action, operator, content, clientIp);
        long elapsed = System.currentTimeMillis() - start;

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("id", id);
        resp.put("elapsedMs", elapsed);
        resp.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(resp);
    }

    /**
     * 分页查询日志列表
     */
    @GetMapping("/mongo/log")
    @Operation(summary = "查询日志列表", description = "按模块分页查询 MongoDB 日志")
    public ResponseEntity<Map<String, Object>> queryLogs(
            @Parameter(description = "模块名") @RequestParam(defaultValue = "TEST") String module,
            @Parameter(description = "页码（0 起始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {

        Page<SystemLog> logs = systemService.queryLogs(module, page, size);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", logs.getContent());
        resp.put("total", logs.getTotalElements());
        resp.put("totalPages", logs.getTotalPages());
        resp.put("currentPage", page);
        return ResponseEntity.ok(resp);
    }

    /**
     * 按 ID 查询单条日志
     */
    @GetMapping("/mongo/log/{id}")
    @Operation(summary = "按 ID 查询日志", description = "根据 MongoDB ObjectId 查询单条日志")
    public ResponseEntity<Map<String, Object>> findById(
            @PathVariable String id) {

        Optional<SystemLog> logOpt = systemService.findById(id);

        Map<String, Object> resp = new HashMap<>();
        if (logOpt.isPresent()) {
            resp.put("success", true);
            resp.put("data", logOpt.get());
        } else {
            resp.put("success", false);
            resp.put("message", "记录不存在: " + id);
        }
        return ResponseEntity.ok(resp);
    }

    // ================================================================
    // ── Redis：缓存读写 ──────────────────────────────────────────────
    // ================================================================

    /**
     * 写入 Redis 缓存（Key-Value 结构，TTL = 5 分钟）
     *
     * <p>请求体示例：{@code {"key":"testKey","value":"hello world"}}
     */
    @PutMapping("/redis/cache")
    @Operation(summary = "写入 Redis 缓存", description = "将键值对写入 Redis，TTL 5 分钟")
    public ResponseEntity<Map<String, Object>> setCache(
            @RequestBody Map<String, Object> body) {

        String key   = (String) body.get("key");
        Object value = body.get("value");

        if (key == null || key.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "key 不能为空"));
        }

        systemService.setCache(key, value);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("key", key);
        resp.put("message", "缓存写入成功，TTL=300s");
        return ResponseEntity.ok(resp);
    }

    /**
     * 读取 Redis 缓存
     */
    @GetMapping("/redis/cache")
    @Operation(summary = "读取 Redis 缓存", description = "根据 key 从 Redis 读取缓存值")
    public ResponseEntity<Map<String, Object>> getCache(
            @RequestParam String key) {

        Object value = systemService.getCache(key);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("key", key);
        resp.put("value", value);
        resp.put("hit", value != null);
        return ResponseEntity.ok(resp);
    }

    // ================================================================
    // ── Redis：原子计数器（高并发计数演示）─────────────────────────
    // ================================================================

    /**
     * 计数器原子自增（每次调用 +1）
     */
    @PostMapping("/redis/counter/incr")
    @Operation(summary = "计数器自增", description = "Redis INCR 原子操作，线程安全，适合高并发计数")
    public ResponseEntity<Map<String, Object>> increment() {
        Long count = systemService.incrementCounter();

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("counter", count);
        resp.put("message", "计数器已自增");
        return ResponseEntity.ok(resp);
    }

    /**
     * 获取当前计数器值
     */
    @GetMapping("/redis/counter")
    @Operation(summary = "读取计数器", description = "返回 Redis 当前计数器值")
    public ResponseEntity<Map<String, Object>> getCounter() {
        Long count = systemService.getCounter();

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("counter", count);
        return ResponseEntity.ok(resp);
    }

    /**
     * 重置计数器为 0
     */
    @DeleteMapping("/redis/counter/reset")
    @Operation(summary = "重置计数器", description = "将 Redis 计数器归零")
    public ResponseEntity<Map<String, Object>> resetCounter() {
        systemService.resetCounter();

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("counter", 0);
        resp.put("message", "计数器已重置为 0");
        return ResponseEntity.ok(resp);
    }

    // ================================================================
    // ── 系统统计快照 ─────────────────────────────────────────────────
    // ================================================================

    /**
     * 获取系统统计快照（含 MongoDB 文档总数、Redis 计数器）
     */
    @GetMapping("/stats")
    @Operation(summary = "系统统计快照", description = "汇总 MongoDB + Redis 当前状态，60s 缓存")
    public ResponseEntity<Map<String, Object>> getStats() {
        String json = systemService.getStatsJson();

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("stats", json);
        resp.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(resp);
    }
}
