package com.quant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class QuantApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuantApplication.class, args);
    }
}
