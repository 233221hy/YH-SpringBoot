package cn.xfywz.guozespring;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableAspectJAutoProxy
@MapperScan("cn.xfywz.guozespring.mapper")
public class GuozeSpringApplication {
    public static void main(String[] args) {
        SpringApplication.run(GuozeSpringApplication.class, args);
    }
}