package ru.mospolytech.library.configs;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import ru.mospolytech.library.security.AccountRefreshFilter;
import ru.mospolytech.library.services.AuditService;

import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain security(
            HttpSecurity http, AuditService audit, ObjectMapper json, UserDetailsService users)
            throws Exception {
        http.addFilterBefore(
                new AccountRefreshFilter(users),
                org.springframework.security.web.access.intercept.AuthorizationFilter.class);
        http.authorizeHttpRequests(
                a ->
                        a.requestMatchers(
                                        "/",
                                        "/index.html",
                                        "/app.js",
                                        "/style.css",
                                        "/api/session",
                                        "/actuator/health",
                                        "/error")
                                .permitAll()
                                .anyRequest()
                                .authenticated());
        http.formLogin(
                f ->
                        f.loginProcessingUrl("/api/login")
                                .successHandler(
                                        (req, res, auth) -> {
                                            res.setContentType("application/json");
                                            json.writeValue(
                                                    res.getOutputStream(), Map.of("ok", true));
                                        })
                                .failureHandler(
                                        (req, res, ex) -> {
                                            audit.failure(
                                                    "AUTHENTICATION_FAILED", req.getRequestURI());
                                            res.setStatus(401);
                                            res.setContentType("application/json;charset=UTF-8");
                                            json.writeValue(
                                                    res.getOutputStream(),
                                                    Map.of(
                                                            "code",
                                                            "AUTHENTICATION_FAILED",
                                                            "message",
                                                            "Неверный логин или пароль."));
                                        }));
        http.logout(
                l ->
                        l.logoutUrl("/api/logout")
                                .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)));
        http.exceptionHandling(
                e ->
                        e.authenticationEntryPoint(
                                        (req, res, ex) -> {
                                            res.setStatus(401);
                                            res.setContentType("application/json;charset=UTF-8");
                                            json.writeValue(
                                                    res.getOutputStream(),
                                                    Map.of(
                                                            "code",
                                                            "AUTHENTICATION_REQUIRED",
                                                            "message",
                                                            "Войдите в систему."));
                                        })
                                .accessDeniedHandler(
                                        (req, res, ex) -> {
                                            audit.failure("ACCESS_DENIED", req.getRequestURI());
                                            res.setStatus(403);
                                            res.setContentType("application/json;charset=UTF-8");
                                            json.writeValue(
                                                    res.getOutputStream(),
                                                    Map.of(
                                                            "code",
                                                            "ACCESS_DENIED",
                                                            "message",
                                                            "Недостаточно прав или устарел защитный"
                                                                + " токен. Обновите страницу."));
                                        }));
        http.headers(
                h ->
                        h.contentSecurityPolicy(
                                c ->
                                        c.policyDirectives(
                                                "default-src 'self'; script-src 'self'; style-src"
                                                    + " 'self'; img-src 'self' data:;"
                                                    + " frame-ancestors 'none'; base-uri 'self';"
                                                    + " form-action 'self'")));
        return http.build();
    }
}
