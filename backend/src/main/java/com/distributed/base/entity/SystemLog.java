package com.distributed.base.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统日志 MongoDB 文档实体
 *
 * <p>对应 MongoDB 集合：{@code system_logs}
 *
 * <p>字段说明：
 * <ul>
 *   <li>id        — MongoDB ObjectId，主键</li>
 *   <li>traceId   — 全链路追踪 ID，便于分布式日志关联</li>
 *   <li>module    — 模块名称（如 USER、ORDER），支持按模块查询</li>
 *   <li>action    — 操作类型（CREATE / READ / UPDATE / DELETE / CALL）</li>
 *   <li>operator  — 操作人标识（用户 ID 或系统名）</li>
 *   <li>content   — 操作详情 / 请求摘要（限 2000 字符）</li>
 *   <li>result    — 操作结果（SUCCESS / FAILURE / UNKNOWN）</li>
 *   <li>elapsed   — 耗时（毫秒），用于性能分析</li>
 *   <li>createdAt — 记录创建时间（自动填充）</li>
 *   <li>updatedAt — 最后更新时间（自动填充）</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "system_logs")
@CompoundIndex(name = "idx_module_action", def = "{'module': 1, 'action': 1}")
@CompoundIndex(name = "idx_operator_created", def = "{'operator': 1, 'createdAt': -1}")
public class SystemLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** MongoDB 主键（ObjectId 字符串形式）*/
    @Id
    private String id;

    /** 全链路追踪 ID */
    @Indexed
    @Field("trace_id")
    private String traceId;

    /** 所属业务模块 */
    @Indexed
    @Field("module")
    private String module;

    /** 操作动作类型：CREATE / READ / UPDATE / DELETE / CALL */
    @Field("action")
    private String action;

    /** 操作人标识 */
    @Field("operator")
    private String operator;

    /** 操作内容摘要（请求参数、接口路径等）*/
    @Field("content")
    private String content;

    /** 操作结果：SUCCESS / FAILURE / UNKNOWN */
    @Field("result")
    private String result;

    /** 接口耗时（毫秒）*/
    @Field("elapsed_ms")
    private Long elapsedMs;

    /** 客户端 IP */
    @Field("client_ip")
    private String clientIp;

    /** 记录创建时间（UTC+8）*/
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    /** 最后更新时间（UTC+8）*/
    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
