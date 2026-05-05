package org.example.contract.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.contract.entity.Comment;
import org.example.contract.service.CommentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CommentController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listByConflict() throws Exception {
        when(commentService.getByConflictId(1L)).thenReturn(List.of());
        mockMvc.perform(get("/api/comments/conflict/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createComment() throws Exception {
        Comment in = new Comment();
        Comment out = new Comment();
        out.setId(5L);
        when(commentService.create(eq(2L), org.mockito.ArgumentMatchers.any(Comment.class))).thenReturn(out);
        mockMvc.perform(post("/api/comments/conflict/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(in)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void updateComment() throws Exception {
        Comment updated = new Comment();
        updated.setId(3L);
        Map<String, String> body = new HashMap<>();
        body.put("content", "hello");
        when(commentService.update(3L, "hello")).thenReturn(updated);
        mockMvc.perform(put("/api/comments/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void deleteComment() throws Exception {
        mockMvc.perform(delete("/api/comments/8"))
                .andExpect(status().isNoContent());
        verify(commentService).delete(8L);
    }
}
