package org.example.offer.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
@Slf4j
public class UserFeignClientFallback implements UserFeignClient {

    @Override
    public Map<String, Object> getUserById(Long id) {
        log.warn("User service unavailable — fallback triggered for userId={}", id);
        return Collections.emptyMap();
    }
}
