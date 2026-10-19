package com.uday.aitesting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AiTestingPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiTestingPlatformApplication.class, args);
    }
}
