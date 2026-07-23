package cn.xfywz.guozespring.config;
import cn.xfywz.guozespring.serializer.PatternBigDecimalSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;

@Configuration
public class JacksonConfig {

    /**
     * BigDecimal 输出时，保留两位小数
     */
    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        // 1. 利用 Spring 注入的 builder (它已经包含了 application.yml 中的基础配置)
        // 2. 显式强制注册 JavaTimeModule (解决 LocalDateTime 问题)
        // 3. 注册 BigDecimal 自定义模块
        return builder
                .modules(new JavaTimeModule(), customBigDecimalModule())
                // 确保日期输出为字符串，而不是时间戳
                .featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    private Module customBigDecimalModule() {
        SimpleModule module = new SimpleModule();
        // 为 BigDecimal 类型注册自定义序列化器
        module.addSerializer(BigDecimal.class, new PatternBigDecimalSerializer("0.00"));
        return module;
    }
}
