package com.legends.profile.controller;

import com.legends.profile.dto.AuthRequest;
import com.legends.profile.dto.AuthResponse;
import com.legends.profile.service.AccountManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * LoginScreen REST endpoint.
 * Exposes profile operations over HTTP on port 5000.
 * With the use of AI
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final AccountManager accountManager;

    public ProfileController(AccountManager accountManager) {
        this.accountManager = accountManager;
    }

    /**
     * POST /api/profile/register
     * Registers a new user account.
     *
     * @param request JSON body: { "username": "...", "password": "..." }
     * @return 200 with AuthResponse on success; 400 if username is taken
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest request) {
        AuthResponse response = accountManager.register(request);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /**
     * POST /api/profile/login
     * Authenticates a user and returns profile data.
     *
     * @param request JSON body: { "username": "...", "password": "..." }
     * @return 200 with AuthResponse on success; 401 on bad credentials
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        AuthResponse response = accountManager.login(request);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(401).body(response);
    }

    /**
     * GET /api/profile/{userId}
     * Returns full profile data for the dashboard.
     *
     * @param userId path variable — the user's primary key
     * @return 200 with profile; 404 if not found
     */
    @GetMapping("/{userId}")
    public ResponseEntity<AuthResponse> getProfile(@PathVariable Long userId) {
        AuthResponse response = accountManager.getProfile(userId);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.notFound().build();
    }

    /**
     * Updates score and ranking after a game event
     * @param userId    path variable
     * @param newScore  query param — score points to add
     * @param ranking   query param — new ranking position
     */
    @PatchMapping("/{userId}/stats")
    public ResponseEntity<AuthResponse> updateStats(
            @PathVariable Long userId,
            @RequestParam int newScore,
            @RequestParam int ranking) {
        AuthResponse response = accountManager.updateStats(userId, newScore, ranking);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.notFound().build();
    }
}
