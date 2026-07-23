package cn.xfywz.guozespring.config;

import cn.xfywz.guozespring.util.ConnectionCleanupInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ConnectionCleanupInterceptor connectionCleanupInterceptor;

    public WebMvcConfig(ConnectionCleanupInterceptor connectionCleanupInterceptor) {
        this.connectionCleanupInterceptor = connectionCleanupInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(connectionCleanupInterceptor)
                .addPathPatterns("/**");
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // 添加字符串到 java.sql.Date 的转换器
        registry.addConverter(new Converter<String, Date>() {
            @Override
            public Date convert(String source) {
                if (source == null || source.trim().isEmpty() || "null".equals(source.trim())) {
                    return null;
                }
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    java.util.Date utilDate = sdf.parse(source.trim());
                    return new Date(utilDate.getTime());
                } catch (ParseException e) {
                    throw new IllegalArgumentException("Invalid date format: " + source + ". Expected format: yyyy-MM-dd", e);
                }
            }
        });

        // 添加字符串到 java.sql.Timestamp 的转换器
        registry.addConverter(new Converter<String, Timestamp>() {
            @Override
            public Timestamp convert(String source) {
                if (source == null || source.trim().isEmpty() || "null".equals(source.trim())) {
                    return null;
                }
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    java.util.Date utilDate = sdf.parse(source.trim());
                    return new Timestamp(utilDate.getTime());
                } catch (ParseException e) {
                    // 尝试只有日期的格式
                    try {
                        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
                        java.util.Date utilDate = sdf2.parse(source.trim());
                        return new Timestamp(utilDate.getTime());
                    } catch (ParseException e2) {
                        throw new IllegalArgumentException("Invalid timestamp format: " + source + ". Expected format: yyyy-MM-dd HH:mm:ss or yyyy-MM-dd", e);
                    }
                }
            }
        });
    }

}