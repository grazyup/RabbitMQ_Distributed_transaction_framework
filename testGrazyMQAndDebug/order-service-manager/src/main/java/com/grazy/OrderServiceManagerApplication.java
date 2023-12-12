package com.grazy;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan(value = "com.grazy" , annotationClass = Mapper.class)
@EnableAsync
public class OrderServiceManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceManagerApplication.class, args);
    }

}
