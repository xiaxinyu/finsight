package com.finsight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.finsight"})
public class FinsightApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinsightApplication.class, args);
    }
}
