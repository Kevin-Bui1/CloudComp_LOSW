package com.legends.profile;

import com.legends.profile.dto.AuthRequest;
import com.legends.profile.dto.AuthResponse;
import com.legends.profile.model.UserProfile;
import com.legends.profile.repository.UserRepository;
import com.legends.profile.service.AccountManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Profile Service (AccountManager).
 *
 * @SpringBootTest loads the full application context so I can test with real
 * Spring beans (including the actual BCrypt encoder and H2 database).
 *
 * @ActiveProfiles("test") switches to the test application.properties which
 * uses an in-memory H2 database instead of MySQL — no external database needed.
 *
 * @Transactional rolls back every test after it runs so each test starts with
 * a clean database and they don't interfere with each other.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProfileServiceTests {

    @Autowired
    private AccountManager accountManager;

    @Autowired
    private UserRepository userRepository;

    // PC-TC01: Basic registration should work for a brand new username
    @Test
    void testRegister_newUser_succeeds() {
        AuthRequest req = new AuthRequest("heroPlayer", "secret123");
        AuthResponse res = accountManager.register(req);

        assertTrue(res.isSuccess(), "Registration should succeed for a new username");
        assertEquals("heroPlayer", res.getUsername());
        assertTrue(userRepository.existsByUsername("heroPlayer"));
    }

    // PC-TC02: Registering with a username that's already taken should fail
    @Test
    void testRegister_duplicateUsername_fails() {
        accountManager.register(new AuthRequest("dupUser", "pass1234"));
        AuthResponse res = accountManager.register(new AuthRequest("dupUser", "otherpass"));

        assertFalse(res.isSuccess(), "Registration should fail when username already exists");
        assertTrue(res.getMessage().contains("already exists"));
    }

    // PC-TC03: Login with the correct password should work
    @Test
    void testLogin_correctCredentials_succeeds() {
        accountManager.register(new AuthRequest("validUser", "mypassword"));
        AuthResponse res = accountManager.login(new AuthRequest("validUser", "mypassword"));

        assertTrue(res.isSuccess(), "Login should succeed with correct credentials");
        assertEquals("validUser", res.getUsername());
        assertNotNull(res.getUserId());
    }

    // PC-TC04: Login with the wrong password should fail
    @Test
    void testLogin_wrongPassword_fails() {
        accountManager.register(new AuthRequest("anotherUser", "correctPass"));
        AuthResponse res = accountManager.login(new AuthRequest("anotherUser", "wrongPass"));

        assertFalse(res.isSuccess(), "Login should fail with incorrect password");
        assertTrue(res.getMessage().contains("Incorrect password"));
    }

    // PC-TC05: Login for a username that doesn't exist should fail
    @Test
    void testLogin_unknownUsername_fails() {
        AuthResponse res = accountManager.login(new AuthRequest("ghost", "anypass"));

        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().contains("not found"));
    }

    // PC-TC06: Registering with an empty username should be rejected
    @Test
    void testRegister_emptyUsername_fails() {
        AuthResponse res = accountManager.register(new AuthRequest("", "pass1234"));

        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().contains("empty"));
    }

    // PC-TC07: Password must be stored as a BCrypt hash, not plain text
    @Test
    void testRegister_passwordIsHashed() {
        accountManager.register(new AuthRequest("hashUser", "plaintext"));
        UserProfile stored = userRepository.findByUsername("hashUser").orElseThrow();

        assertNotEquals("plaintext", stored.getPasswordHash(),
                "Password must not be stored in plain text");
        assertTrue(stored.getPasswordHash().startsWith("$2a$"),
                "Password hash should be BCrypt format");
    }

    // PC-TC08: After login, the response should include all profile fields
    @Test
    void testLogin_returnsProfileData() {
        accountManager.register(new AuthRequest("profileUser", "pass9999"));
        AuthResponse res = accountManager.login(new AuthRequest("profileUser", "pass9999"));

        assertTrue(res.isSuccess());
        assertEquals(0, res.getScores());
        assertEquals(0, res.getRankings());
        assertEquals(0, res.getCampaignProgress());
    }
}
