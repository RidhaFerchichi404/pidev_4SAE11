package com.esprit.user.service;

import com.esprit.user.client.KeycloakAuthClient;
import com.esprit.user.dto.UserRequest;
import com.esprit.user.dto.UserUpdateRequest;
import com.esprit.user.entity.Role;
import com.esprit.user.entity.User;
import com.esprit.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private KeycloakAuthClient keycloakAuthClient;
    @InjectMocks
    private UserService userService;

    @Test
    void createReturnsExistingUserWhenEmailAlreadyExists() {
        UserRequest request = new UserRequest();
        request.setEmail("existing@mail.com");
        User existing = new User();
        existing.setEmail("existing@mail.com");
        when(userRepository.findByEmailIgnoreCase("existing@mail.com")).thenReturn(Optional.of(existing));

        User result = userService.create(request);
        assertEquals("existing@mail.com", result.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createSavesUserWithEncodedPassword() {
        UserRequest request = new UserRequest();
        request.setEmail("new@mail.com");
        request.setPassword("secret");
        request.setRole(Role.CLIENT);
        when(userRepository.findByEmailIgnoreCase("new@mail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.create(request);
        assertEquals("new@mail.com", saved.getEmail());
        assertEquals("hashed", saved.getPasswordHash());
    }

    @Test
    void updateThrowsWhenEmailExists() {
        User user = new User();
        user.setId(1L);
        user.setEmail("old@mail.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@mail.com")).thenReturn(true);
        UserUpdateRequest req = new UserUpdateRequest();
        req.setEmail("new@mail.com");

        assertThrows(RuntimeException.class, () -> userService.update(1L, req));
    }

    @Test
    void deleteByIdDoesNothingWhenUserAbsent() {
        when(userRepository.existsById(1L)).thenReturn(false);
        userService.deleteById(1L);
        verify(keycloakAuthClient, never()).deleteUserByEmail(any(String.class));
    }

    @Test
    void findMethodsDelegateToRepository() {
        User user = new User();
        user.setId(10L);
        user.setEmail("mail@test.com");
        user.setRole(Role.ADMIN);
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(user));
        when(userRepository.findByEmail("mail@test.com")).thenReturn(Optional.of(user));

        assertEquals(1, userService.findAll().size());
        assertEquals("mail@test.com", userService.findById(10L).getEmail());
        assertEquals(1, userService.findByRole(Role.ADMIN).size());
        assertEquals("mail@test.com", userService.findByEmail("mail@test.com").getEmail());
    }

    @Test
    void updateUserAndSyncToKeycloak() {
        User user = new User();
        user.setId(2L);
        user.setEmail("old@mail.com");
        user.setRole(Role.CLIENT);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("newpass")).thenReturn("newhash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserUpdateRequest request = new UserUpdateRequest();
        request.setEmail("new@mail.com");
        request.setPassword("newpass");
        request.setRole(Role.FREELANCER);

        User updated = userService.update(2L, request);
        assertEquals("new@mail.com", updated.getEmail());
        verify(keycloakAuthClient).updateUserByEmail("old@mail.com", null, null, "new@mail.com", "FREELANCER");
    }

    @Test
    void deleteByIdRemovesUserAndSyncsKeycloak() {
        User user = new User();
        user.setId(8L);
        user.setEmail("del@mail.com");
        when(userRepository.existsById(8L)).thenReturn(true);
        when(userRepository.findById(8L)).thenReturn(Optional.of(user));

        userService.deleteById(8L);
        verify(userRepository).deleteById(8L);
        verify(keycloakAuthClient).deleteUserByEmail("del@mail.com");
    }
}
