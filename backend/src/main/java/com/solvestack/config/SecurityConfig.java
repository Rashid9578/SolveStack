package com.solvestack.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solvestack.repository.TrackerOwnerRepository;
import com.solvestack.security.GuestSessionFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, SecurityContextRepository contexts,
            TrackerOwnerRepository owners, ObjectMapper json) throws Exception {
        return http
                .securityContext(context -> context.securityContextRepository(contexts))
                .addFilterAfter(new GuestSessionFilter(owners), SecurityContextHolderFilter.class)
                .requestCache(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, failure) ->
                                writeError(json, response, HttpStatus.UNAUTHORIZED, "An active session is required."))
                        .accessDeniedHandler((request, response, failure) ->
                                writeError(json, response, HttpStatus.FORBIDDEN, "The request could not be authorized.")))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/topics", "/api/topics/*").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/api/topics", "/api/topics/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/session", "/api/session/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/session/guest", "/api/session/register",
                                "/api/session/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/session/logout").authenticated()
                        .anyRequest().denyAll())
                .build();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(), new HttpSessionSecurityContextRepository());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    private static void writeError(ObjectMapper json, HttpServletResponse response, HttpStatus status, String detail)
            throws IOException {
        response.setStatus(status.value());
        response.setHeader("Cache-Control", "no-store");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(response.getOutputStream(), ProblemDetail.forStatusAndDetail(status, detail));
    }
}
