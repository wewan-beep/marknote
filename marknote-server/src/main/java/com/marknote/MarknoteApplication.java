package com.marknote;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.marknote.mapper")
public class MarknoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarknoteApplication.class, args);
        System.out.println("MarkNote 后端启动成功: http://localhost:8080");
    }
}
