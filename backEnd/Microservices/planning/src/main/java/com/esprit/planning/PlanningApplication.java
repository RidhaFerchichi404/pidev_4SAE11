package com.esprit.planning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.esprit.planning.client")
@EnableAsync
public class PlanningApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlanningApplication.class, args);
    }

}
