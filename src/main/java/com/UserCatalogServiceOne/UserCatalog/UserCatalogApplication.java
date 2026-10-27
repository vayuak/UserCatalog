package com.UserCatalogServiceOne.UserCatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableAsync
@EnableCaching
@EnableJpaRepositories(basePackages = "com.UserCatalogServiceOne.UserCatalog.Repositories")
public class UserCatalogApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserCatalogApplication.class, args);
    }
}