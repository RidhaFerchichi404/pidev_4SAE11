package org.example.offer.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Client Feign pour le microservice User.
 * Utilisé pour récupérer l'email du freelancer avant d'envoyer un email de notification.
 */
@FeignClient(
        name = "USER",
        path = "/api/users",
        fallback = UserFeignClientFallback.class
)
public interface UserFeignClient {

    @GetMapping("/{id}")
    Map<String, Object> getUserById(@PathVariable("id") Long id);
}
