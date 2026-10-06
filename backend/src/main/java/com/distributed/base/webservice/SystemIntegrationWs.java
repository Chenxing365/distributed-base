package com.distributed.base.webservice;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

/**
 * SystemIntegrationService —— WebService (SOAP) 契约接口
 *
 * <p>规范：JAX-WS 标准注解，CXF 框架实现。
 *
 * <p>WSDL 访问地址（本地）：
 * {@code http://localhost:8080/ws/SystemIntegrationService?wsdl}
 *
 * <p>架构设计：该接口的实现类注入了与 REST Controller 共享的同一
 * {@code SystemLogService} Bean，保证业务逻辑唯一入口，
 * 避免 SOAP 与 REST 双通道产生数据不一致。
 */
@WebService(
        name            = "SystemIntegrationService",
        targetNamespace = "http://webservice.base.distributed.com/"
)
public interface SystemIntegrationWs {

    /**
     * 健康探针：验证 WebService 可达性
     *
     * @return "pong" 字符串
     */
    @WebMethod(operationName = "ping")
    @WebResult(name = "pingResult")
    String ping();

    /**
     * 写入系统日志（通过 SOAP 通道调用）
     *
     * @param module   模块名
     * @param action   操作类型
     * @param operator 操作人
     * @param content  内容摘要
     * @return 新建记录的 MongoDB ObjectId
     */
    @WebMethod(operationName = "writeLog")
    @WebResult(name = "logId")
    String writeLog(
            @WebParam(name = "module")   String module,
            @WebParam(name = "action")   String action,
            @WebParam(name = "operator") String operator,
            @WebParam(name = "content")  String content
    );

    /**
     * 查询指定模块最近 20 条日志摘要（JSON 字符串返回）
     *
     * @param module 模块名
     * @return JSON 数组字符串
     */
    @WebMethod(operationName = "queryRecentLogs")
    @WebResult(name = "logsJson")
    String queryRecentLogs(@WebParam(name = "module") String module);

    /**
     * 获取系统集成统计快照
     *
     * @return JSON 格式统计报文（MongoDB 总数 / Redis 计数器当前值）
     */
    @WebMethod(operationName = "getSystemStats")
    @WebResult(name = "statsJson")
    String getSystemStats();
}
