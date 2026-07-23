package cn.xfywz.guozespring.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 */
@Configuration
public class RedisConfig {
    /**
     * 配置RedisTemplate以支持序列化和反序列化复杂对象
     *
     * @param redisConnectionFactory Redis连接工厂
     * @return 配置好的RedisTemplate实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // 创建RedisTemplate实例
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 设置连接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 创建ObjectMapper实例，用于JSON序列化和反序列化
        ObjectMapper objectMapper = new ObjectMapper();
        // 配置ObjectMapper可见性，允许访问所有属性
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 激活默认类型检测，使用包装数组形式
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY);
        // 创建Jackson2JsonRedisSerializer实例，用于Redis序列化和反序列化
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // 创建StringRedisSerializer实例，用于字符串序列化和反序列化
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        // 设置RedisTemplate的键序列化器
        redisTemplate.setKeySerializer(stringRedisSerializer);
        // 设置RedisTemplate的值序列化器
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        // 设置RedisTemplate的哈希键序列化器
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        // 设置RedisTemplate的哈希值序列化器
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
        // 初始化RedisTemplate
        redisTemplate.afterPropertiesSet();
        // 返回配置好的RedisTemplate实例
        return redisTemplate;
    }
    /**
     * 显式注册 StringRedisTemplate（用于 String -> String 操作）
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
