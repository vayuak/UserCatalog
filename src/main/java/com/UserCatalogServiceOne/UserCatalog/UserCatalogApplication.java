package com.UserCatalogServiceOne.UserCatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // 🟢 ENABLES BACKGROUND THREADS FOR EMAIL SENDING
@EnableCaching
public class UserCatalogApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserCatalogApplication.class, args);
    }
}