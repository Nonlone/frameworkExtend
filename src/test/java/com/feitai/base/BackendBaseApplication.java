package com.feitai.base;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;


@SpringBootApplication
@MapperScan(basePackages = {"com.feitai.base.mapper"})
public class BackendBaseApplication {
    // 入口
    public static void main(String[] args) {
        SpringApplication.run(BackendBaseApplication.class, args);
    }

}
