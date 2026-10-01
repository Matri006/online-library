package ru.mospolytech.library.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Re-evaluate account status and role on each request, including existing sessions. */
public class AccountRefreshFilter extends OncePerRequestFilter {
    private final UserDetailsService users;

    public AccountRefreshFilter(UserDetailsService users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        var context = SecurityContextHolder.getContext();
        var auth = context.getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails previous) {
            try {
                var current = users.loadUserByUsername(auth.getName());
                if (!current.isEnabled()) {
                    context.setAuthentication(null);
                    if (req.getSession(false) != null) req.getSession(false).invalidate();
                } else {
                    var refreshed =
                            UsernamePasswordAuthenticationToken.authenticated(
                                    current, null, current.getAuthorities());
                    refreshed.eraseCredentials();
                    context.setAuthentication(refreshed);
                }
            } catch (UsernameNotFoundException ex) {
                context.setAuthentication(null);
                if (req.getSession(false) != null) req.getSession(false).invalidate();
            }
        }
        chain.doFilter(req, res);
    }
}
