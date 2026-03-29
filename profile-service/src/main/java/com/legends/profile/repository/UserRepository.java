package com.legends.profile.repository;

import com.legends.profile.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * UserRepository is our database access layer for user accounts.
 *
 * By extending JpaRepository, Spring Data automatically generates all the basic
 * SQL operations (save, findById, delete, etc.) at runtime — I don't have to
 * write any SQL myself for those.
 *
 * I only need to declare the two custom queries I actually use:
 *   existsByUsername — for the duplicate check during registration
 *   findByUsername   — for login (we look up by username, not ID)
 *
 * Spring figures out the SQL from the method names automatically.
 */
public interface UserRepository extends JpaRepository<UserProfile, Long> {

    boolean existsByUsername(String username);

    Optional<UserProfile> findByUsername(String username);
}
