package com.legends.profile.service;

import com.legends.profile.dto.AuthRequest;
import com.legends.profile.dto.AuthResponse;
import com.legends.profile.model.UserProfile;
import com.legends.profile.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * AccountManager handles all the logic around user accounts.
 *
 * I separated this from the controller so the controller stays thin —
 * it just takes requests in and sends responses back, while all the actual
 * decision-making (is this username taken? does this password match?) lives here.
 *
 * This is the "Facade" pattern mentioned in our SDD: the controller doesn't need to
 * know anything about the database or password hashing, it just calls these methods.
 */
@Service
public class AccountManager {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountManager(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user account.
     *
     * Checks:
     *  - username can't be empty
     *  - password must be at least 4 characters
     *  - username must not already be taken
     *
     * The password is hashed with BCrypt before being saved — we never store plain text.
     */
    public AuthResponse register(AuthRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return AuthResponse.error("Username cannot be empty.");
        }
        if (request.getPassword() == null || request.getPassword().length() < 4) {
            return AuthResponse.error("Password must be at least 4 characters.");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            return AuthResponse.error("Username already exists.");
        }
        UserProfile profile = new UserProfile(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword())
        );
        userRepository.save(profile);
        return AuthResponse.ok(profile.getUserId(), profile.getUsername(),
                profile.getScores(), profile.getRankings(), profile.getCampaignProgress());
    }

    /**
     * Logs a user in by checking their password against the stored BCrypt hash.
     *
     * passwordEncoder.matches() handles the BCrypt comparison — we never
     * decrypt the hash, we just re-hash the input and compare.
     */
    public AuthResponse login(AuthRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .map(profile -> {
                    if (passwordEncoder.matches(request.getPassword(), profile.getPasswordHash())) {
                        return AuthResponse.ok(profile.getUserId(), profile.getUsername(),
                                profile.getScores(), profile.getRankings(), profile.getCampaignProgress());
                    }
                    return AuthResponse.error("Incorrect password.");
                })
                .orElse(AuthResponse.error("Username not found."));
    }

    /**
     * Returns profile data by userId — used when the player opens their profile screen.
     */
    public AuthResponse getProfile(Long userId) {
        return userRepository.findById(userId)
                .map(p -> AuthResponse.ok(p.getUserId(), p.getUsername(),
                        p.getScores(), p.getRankings(), p.getCampaignProgress()))
                .orElse(AuthResponse.error("User not found."));
    }

    /**
     * Updates score and ranking after a campaign finishes or a PvP match ends.
     * Score is additive (keeps accumulating), ranking is replaced with the new value.
     */
    public AuthResponse updateStats(Long userId, int newScore, int ranking) {
        return userRepository.findById(userId).map(p -> {
            p.setScores(p.getScores() + newScore);
            p.setRankings(ranking);
            userRepository.save(p);
            return AuthResponse.ok(p.getUserId(), p.getUsername(),
                    p.getScores(), p.getRankings(), p.getCampaignProgress());
        }).orElse(AuthResponse.error("User not found."));
    }
}
