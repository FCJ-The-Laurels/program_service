package com.smokefree.program.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class HeaderUserContextFilter extends OncePerRequestFilter {

    private final Environment env;

    public HeaderUserContextFilter(Environment env) {
        this.env = env;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest req,
            @NonNull HttpServletResponse res,
            @NonNull FilterChain chain) throws ServletException, IOException {

        String uid  = req.getHeader("X-User-Id");
        String role = req.getHeader("X-User-Role");

        // Nếu đã có Authentication thì bỏ qua
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(req, res);
            return;
        }

        boolean isDev = Arrays.asList(env.getActiveProfiles()).contains("dev");

        // Thiếu header
        if (uid == null || uid.isBlank() || role == null || role.isBlank()) {
            if (isDev) { // dev thì cho qua
                chain.doFilter(req, res);
                return;
            }
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");
            res.getWriter().write(
                    "{\"error\":\"unauthorized\",\"message\":\"Missing or invalid X-User-Id / X-User-Role\"}"
            );
            return;
        }

        // Có header → validate & set Authentication
        try {
            UUID userId = UUID.fromString(uid.trim());
            var auth = new UsernamePasswordAuthenticationToken(
                    userId.toString(),
                    "N/A",
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.trim().toUpperCase()))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (IllegalArgumentException ex) {
            if (isDev) { // dev thì cho qua
                chain.doFilter(req, res);
                return;
            }
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");
            res.getWriter().write(
                    "{\"error\":\"unauthorized\",\"message\":\"X-User-Id must be a valid UUID\"}"
            );
            return;
        }

        chain.doFilter(req, res);
    }
}
