package ru.mospolytech.library.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.mospolytech.library.dto.Responses;

@RestController
@RequestMapping("/api/session")
public class SessionController {
    @GetMapping
    public Responses.Session session(Authentication authentication, CsrfToken token) {
        String login = authentication == null ? "" : authentication.getName();
        String role =
                authentication == null
                        ? ""
                        : authentication.getAuthorities().stream()
                                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                                .findFirst()
                                .orElse("");
        return new Responses.Session(
                authentication != null, login, role, token.getToken(), token.getHeaderName());
    }
}
