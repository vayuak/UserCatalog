package com.UserCatalogServiceOne.UserCatalog.Configurations;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import redis.embedded.RedisServer;

import java.io.IOException;

@Configuration
@Slf4j
public class EmbeddedRedisConfig {
    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() {
        try {
            redisServer = new RedisServer(6379);
            redisServer.start();
            log.info(">>> GHOST SHIELD: Volatile In-Memory Cache Engine active on Port 6379");
        } catch (Exception e) {
            log.warn(">>> GHOST SHIELD: Memory cluster interface bound or active: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            try {
                redisServer.stop();
                log.info(">>> GHOST SHIELD: Volatile In-Memory Cache Engine successfully shut down.");
            } catch (IOException e) {
                log.error(">>> GHOST SHIELD: Error while shutting down memory cache: {}", e.getMessage());
            }
        }
    }
}