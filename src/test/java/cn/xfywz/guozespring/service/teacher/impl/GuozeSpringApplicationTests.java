package cn.xfywz.guozespring.service.teacher.impl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class GuozeSpringApplicationTests {

    @Test
    void contextLoads() {
    }
    @Test
    void password(){
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        String password = "Abc123456";
        password = bCryptPasswordEncoder.encode(password);
        System.out.println("生成的密码是："+ password);
    }

}