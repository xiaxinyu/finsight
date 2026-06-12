package com.finsight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication(scanBasePackages = {"com.finsight"})
public class FinsightApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinsightApplication.class, args);
    }
}
