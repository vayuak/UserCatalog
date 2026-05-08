package com.UserCatalogServiceOne.UserCatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication

@EnableDiscoveryClient
@EnableJpaRepositories(basePackages = "com.UserCatalogServiceOne.UserCatalog.Repositories")
public class UserCatalogApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserCatalogApplication.class, args);

        System.out.println("              app started successfully !       ");
	}

}
