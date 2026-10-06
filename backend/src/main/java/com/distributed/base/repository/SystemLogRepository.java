package com.distributed.base.repository;

import com.distributed.base.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * SystemLog MongoDB 数据访问层
 *
 * <p>继承 {@link MongoRepository} 自动获得 CRUD + 分页方法。
 * 自定义查询方法遵循 Spring Data 命名约定，框架自动解析生成查询语句。
 */
@Repository
public interface SystemLogRepository extends MongoRepository<SystemLog, String> {

    /**
     * 按模块查询（分页）
     *
     * @param module   模块名
     * @param pageable 分页参数
     */
    Page<SystemLog> findByModuleOrderByCreatedAtDesc(String module, Pageable pageable);

    /**
     * 按操作人与时间范围查询
     */
    List<SystemLog> findByOperatorAndCreatedAtBetween(
            String operator,
            LocalDateTime from,
            LocalDateTime to
    );

    /**
     * 按 traceId 精确查询
     */
    Optional<SystemLog> findByTraceId(String traceId);

    /**
     * 按结果统计数量（SUCCESS / FAILURE）
     */
    long countByResult(String result);

    /**
     * 自定义 JSON 查询：查找耗时超过阈值且操作失败的日志
     *
     * @param elapsedMs  耗时阈值（毫秒）
     */
    @Query("{ 'elapsed_ms': { $gt: ?0 }, 'result': 'FAILURE' }")
    List<SystemLog> findSlowFailedLogs(long elapsedMs);

    /**
     * 按模块和动作类型查询最近 N 条
     */
    List<SystemLog> findTop20ByModuleAndActionOrderByCreatedAtDesc(String module, String action);
}
