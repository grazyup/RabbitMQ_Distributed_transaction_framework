package com.grazy;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan(value = "com.grazy" , annotationClass = Mapper.class)
public class GrazyMqApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrazyMqApplication.class, args);
    }

}
