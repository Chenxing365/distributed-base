package com.distributed.base.webservice.impl;

import com.distributed.base.entity.SystemLog;
import com.distributed.base.service.SystemService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.jws.WebService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.distributed.base.webservice.SystemIntegrationWs;

import java.util.List;

/**
 * SystemIntegrationWs SOAP 实现类
 *
 * <p>通过注入 {@link SystemService} 共享同一业务逻辑，
 * REST 接口与 SOAP 接口数据完全一致，无双写风险。
 *
 * <p>注意：{@code @Component} 使其成为 Spring Bean，
 * {@code @WebService(endpointInterface)} 声明实现关系让 CXF 识别。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@WebService(
        serviceName       = "SystemIntegrationService",
        portName          = "SystemIntegrationServicePort",
        targetNamespace   = "http://webservice.base.distributed.com/",
        endpointInterface = "com.distributed.base.webservice.SystemIntegrationWs"
)
public class SystemIntegrationWsImpl implements SystemIntegrationWs {

    private final SystemService systemService;

    /** Jackson 用于将对象列表序列化为 JSON 字符串返回给 SOAP 客户端 */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    // ── ping ─────────────────────────────────────────────────────────
    @Override
    public String ping() {
        log.info("[SOAP] ping 调用");
        return "pong — distributed-base WebService is alive at " + java.time.LocalDateTime.now();
    }

    // ── writeLog ─────────────────────────────────────────────────────
    @Override
    public String writeLog(String module, String action, String operator, String content) {
        log.info("[SOAP] writeLog 调用: module={}, action={}, operator={}", module, action, operator);
        return systemService.saveLog(module, action, operator, content, "SOAP-CLIENT");
    }

    // ── queryRecentLogs ───────────────────────────────────────────────
    @Override
    public String queryRecentLogs(String module) {
        log.info("[SOAP] queryRecentLogs 调用: module={}", module);
        List<SystemLog> logs = systemService.findByModule(module);
        try {
            return objectMapper.writeValueAsString(logs);
        } catch (JsonProcessingException e) {
            log.error("[SOAP] 序列化日志列表失败", e);
            return "[]";
        }
    }

    // ── getSystemStats ────────────────────────────────────────────────
    @Override
    public String getSystemStats() {
        log.info("[SOAP] getSystemStats 调用");
        return systemService.getStatsJson();
    }
}
