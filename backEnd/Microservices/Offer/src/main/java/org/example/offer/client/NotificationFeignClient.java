package org.example.offer.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "NOTIFICATION",
        path = "/api/notifications"
)
public interface NotificationFeignClient {

    @PostMapping
    Map<String, Object> create(@RequestBody Map<String, Object> request);
}
