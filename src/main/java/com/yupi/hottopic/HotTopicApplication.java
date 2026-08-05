package com.yupi.hottopic;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.yupi.hottopic.mapper")
@EnableScheduling
public class HotTopicApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotTopicApplication.class, args);
    }

}
