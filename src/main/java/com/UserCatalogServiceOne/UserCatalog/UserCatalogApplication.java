package com.UserCatalogServiceOne.UserCatalog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;

@SpringBootApplication(exclude = {
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class
})
@Slf4j
public class UserCatalogApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserCatalogApplication.class, args);
        log.info("   USER-CATALOG-SERVICE ready for frontend connection layer !");

    }
}