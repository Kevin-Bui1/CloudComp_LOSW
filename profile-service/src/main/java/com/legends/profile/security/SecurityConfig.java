package com.legends.profile.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the profile service.
 *
 * Two things this does:
 *
 * 1. Disables CSRF protection — CSRF is a browser-based attack that doesn't
 *    apply to a REST API because we're not using cookies or browser sessions.
 *    Leaving it enabled would break all our POST requests without any benefit.
 *
 * 2. Sets up BCrypt password hashing with strength 12 — this means the hashing
 *    algorithm runs 2^12 = 4096 iterations, which makes brute-force attacks slow.
 *    Strength 12 is the industry standard for this.
 *
 * The PasswordEncoder @Bean is a Singleton — Spring creates one instance and
 * injects it wherever it's needed (in AccountManager in this case).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
