package com.airelay;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.airelay.**.mapper")
@EnableScheduling
public class AiRelayApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiRelayApplication.class, args);
    }
}
