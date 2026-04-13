package com.stufamily.backend.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.stufamily.backend")
public class StuFamilyBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(StuFamilyBackendApplication.class, args);
    }
}

