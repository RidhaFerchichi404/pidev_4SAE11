package com.esprit.keycloak.client;

import com.esprit.keycloak.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class UserServiceClientTest {

    @Test
    void createUserPostsToUserService() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(eq("http://user/api/users"), any(HttpEntity.class), eq(Object.class)))
            .thenReturn(ResponseEntity.ok().build());
        UserServiceClient client = new UserServiceClient(restTemplate);
        RegisterRequest request = new RegisterRequest();
        request.setEmail("x@y.com");
        request.setPassword("password");
        request.setFirstName("x");
        request.setLastName("y");
        request.setRole("client");

        client.createUser(request);
        verify(restTemplate).postForEntity(eq("http://user/api/users"), any(HttpEntity.class), eq(Object.class));
    }

    @Test
    void createUserWrapsErrors() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        doThrow(new RuntimeException("boom")).when(restTemplate)
            .postForEntity(eq("http://user/api/users"), any(HttpEntity.class), eq(Object.class));
        UserServiceClient client = new UserServiceClient(restTemplate);
        RegisterRequest request = new RegisterRequest();
        request.setEmail("x@y.com");
        request.setPassword("password");
        request.setRole("client");

        assertThrows(IllegalStateException.class, () -> client.createUser(request));
    }
}
