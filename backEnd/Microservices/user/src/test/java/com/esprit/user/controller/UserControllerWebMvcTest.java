package com.esprit.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.esprit.user.dto.UserRequest;
import com.esprit.user.dto.UserUpdateRequest;
import com.esprit.user.entity.Role;
import com.esprit.user.entity.User;
import com.esprit.user.service.UserService;

@WebMvcTest(value = UserController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@TestPropertySource(properties = "welcome.message=Hello test")
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    void welcome() throws Exception {
        mockMvc.perform(get("/api/users/welcome"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello test"));
    }

    @Test
    void getAllUsers() throws Exception {
        when(userService.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getUserById() throws Exception {
        User u = new User();
        u.setId(1L);
        u.setEmail("a@b.com");
        u.setFirstName("A");
        u.setLastName("B");
        u.setRole(Role.FREELANCER);
        when(userService.findById(1L)).thenReturn(u);
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getUserByEmail() throws Exception {
        User u = new User();
        u.setId(2L);
        u.setEmail("x@y.com");
        u.setFirstName("X");
        u.setLastName("Y");
        u.setRole(Role.CLIENT);
        when(userService.findByEmail("x@y.com")).thenReturn(u);
        mockMvc.perform(get("/api/users/email/x@y.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("x@y.com"));
    }

    @Test
    void createUser() throws Exception {
        UserRequest req = new UserRequest();
        req.setEmail("new@test.com");
        req.setPassword("secret12345");
        req.setFirstName("N");
        req.setLastName("W");
        req.setRole(Role.FREELANCER);
        User saved = new User();
        saved.setId(9L);
        saved.setEmail("new@test.com");
        saved.setFirstName("N");
        saved.setLastName("W");
        saved.setRole(Role.FREELANCER);
        when(userService.create(any(UserRequest.class))).thenReturn(saved);
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void updateUser() throws Exception {
        UserUpdateRequest req = new UserUpdateRequest();
        req.setEmail("up@test.com");
        User out = new User();
        out.setId(3L);
        out.setEmail("up@test.com");
        out.setFirstName("F");
        out.setLastName("L");
        out.setRole(Role.CLIENT);
        when(userService.update(eq(3L), any(UserUpdateRequest.class))).thenReturn(out);
        mockMvc.perform(put("/api/users/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void getUsersByRole() throws Exception {
        when(userService.findByRole(Role.ADMIN)).thenReturn(List.of());
        mockMvc.perform(get("/api/users/by-role").param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getUsersByRoleInvalidValueReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/users/by-role").param("role", "UNKNOWN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deleteUserReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/5"))
            .andExpect(status().isNoContent());
    }

    @Test
    void getAllUsersReturnsEmptyOnServiceFailure() throws Exception {
        doThrow(new RuntimeException("db down")).when(userService).findAll();
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
