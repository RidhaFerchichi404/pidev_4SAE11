package com.esprit.user.repository;

import com.esprit.user.entity.Role;
import com.esprit.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity persistence.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByIsActive(Boolean isActive);
}
