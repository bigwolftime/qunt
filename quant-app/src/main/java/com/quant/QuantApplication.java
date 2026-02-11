package com.quant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
@MapperScan("com.quant.persistence.mapper")
public class QuantApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuantApplication.class, args);
    }
}
