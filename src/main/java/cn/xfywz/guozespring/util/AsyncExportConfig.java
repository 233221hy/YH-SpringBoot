package cn.xfywz.guozespring.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncExportConfig {

    @Bean("exportExecutor")
    public Executor exportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心：同一时间最多【3个导出】（保护数据库）
        executor.setCorePoolSize(3);

        // 最大也【3个】，不扩容，避免DB压力飙升
        executor.setMaxPoolSize(3);

        // 队列可以排队 30~50 个，足够用
        executor.setQueueCapacity(40);

        // 线程空闲多久回收（无所谓）
        executor.setKeepAliveSeconds(60);

        // 线程名称
        executor.setThreadNamePrefix("export-task-");

        // 拒绝策略：满了直接抛出异常（最安全，不影响主线程）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

        executor.initialize();
        return executor;
    }

    @Bean("practiceReportExportExecutor")
    public Executor practiceReportExportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("practice-report-export-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}