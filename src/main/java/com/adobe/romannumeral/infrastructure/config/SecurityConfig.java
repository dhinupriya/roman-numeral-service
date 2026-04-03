package com.adobe.romannumeral.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration — defense-in-depth via SecurityFilterChain.
 *
 * <p>Configures:
 * <ul>
 *   <li><b>Security headers</b> — X-Content-Type-Options: nosniff, X-Frame-Options: DENY,
 *       Cache-Control: no-store, Strict-Transport-Security (HSTS), Referrer-Policy</li>
 *   <li><b>CORS</b> — deny all cross-origin by default (configurable)</li>
 *   <li><b>Stateless sessions</b> — no HTTP session, no CSRF (API-only service)</li>
 *   <li><b>Authorization</b> — permits all requests (auth handled by ApiKeyAuthFilter)</li>
 * </ul>
 *
 * <p>API key authentication and rate limiting are handled by separate servlet filters
 * (ApiKeyAuthFilter, RateLimitFilter) that run before the SecurityFilterChain.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .headers(headers -> headers
                        .contentTypeOptions(content -> {})  // X-Content-Type-Options: nosniff
                        .frameOptions(frame -> frame.deny()) // X-Frame-Options: DENY
                        .cacheControl(cache -> {})           // Cache-Control: no-store
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))  // HSTS: 1 year
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .build();
    }

    /**
     * CORS configuration — deny all cross-origin by default.
     * In production, configure allowed origins via environment variables.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of()); // deny all by default
        config.setAllowedMethods(List.of("GET"));
        config.setAllowedHeaders(List.of("X-API-Key", "X-Correlation-Id"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/romannumeral/**", config);
        return source;
    }
}
