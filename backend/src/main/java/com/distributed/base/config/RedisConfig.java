package com.distributed.base.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 *
 * <p>解决问题：
 * <ul>
 *   <li>Key 序列化为可读字符串（StringRedisSerializer），避免前缀乱码</li>
 *   <li>Value 序列化为 JSON（GenericJackson2JsonRedisSerializer），支持对象存取</li>
 *   <li>内置类型信息，反序列化时自动还原原始类型</li>
 *   <li>JavaTimeModule 支持 LocalDateTime 等 Java 8+ 时间类型</li>
 * </ul>
 */
@Configuration
public class RedisConfig {

    /**
     * 通用 RedisTemplate（Key=String, Value=任意 Java 对象，以 JSON 存储）
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {

        // ── Jackson ObjectMapper：支持多态类型信息 ──────────────
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 将完整类名写入 JSON，反序列化时可自动还原类型
        om.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // Java 8+ 时间模块，防止 LocalDateTime 被序列化为数组
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(om);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // ── 装配 Template ──────────────────────────────────────
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key / HashKey：String 序列化（可读）
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value / HashValue：JSON 序列化
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * StringRedisTemplate：纯字符串操作（计数器、Token 等简单场景）
     * Spring Boot 自动配置已提供，此处显式声明便于注入说明。
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
