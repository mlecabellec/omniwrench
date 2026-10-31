package com.omniwrench.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Objects;

/**
 * Spring Security configuration permitting headless REST endpoints, WebSocket streaming, and Web HUD assets.
 *
 * Traceability:
 * - Requirement: REQ-00050 (Headless HTTP/REST &amp; WebSocket Agent Server API)
 * - Feature: FR-00015 (RESTful Agent Dialogue &amp; Session Inspection API)
 * - Use Case: UC-00003 (Web UI Agent Collaboration)
 * - Task: TSK-20260822-004 (Spring Web &amp; Reactive WebSocket Server Engine)
 * - ADR: ADR-0003 (Spring Web MVC &amp; Reactive WebSocket Streaming Architecture)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures the main security filter chain allowing unauthenticated access to the Web HUD and APIs.
     *
     * @param http the HttpSecurity builder, must not be null
     * @return configured SecurityFilterChain
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        final HttpSecurity nonNullHttp = Objects.requireNonNull(http, "http must not be null");

        nonNullHttp
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/favicon.ico",
                                "/css/**",
                                "/js/**",
                                "/assets/**",
                                "/api/v1/**",
                                "/ws/**"
                        ).permitAll()
                        .anyRequest().permitAll()
                );

        return nonNullHttp.build();
    }
}
