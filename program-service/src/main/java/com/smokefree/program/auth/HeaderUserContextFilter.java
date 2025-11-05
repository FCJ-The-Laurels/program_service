package com.smokefree.program.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

public class HeaderUserContextFilter extends OncePerRequestFilter {
    private final Environment env;
    public HeaderUserContextFilter(Environment env) { this.env = env; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // Nếu đã có auth (do filter khác set) thì thôi
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(req, res); return;
        }

        String uid = req.getHeader("X-User-Id");
        String role = req.getHeader("X-User-Role");

        boolean isDev = Arrays.asList(env.getActiveProfiles()).contains("dev");

        // Thiếu header:
        if (uid == null || role == null || uid.isBlank() || role.isBlank()) {
            if (isDev) { // dev: nhường cho DevAutoUserFilter
                chain.doFilter(req, res); return;
            }
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");
            res.getWriter().write("""
        {"error":"unauthorized","message":"Missing or invalid X-User-Id / X-User-Role"}
        """);
            return;
        }

        // Có header thì cứ để DevAutoUserFilter set auth hoặc controller xử lý tiếp
        // (hoặc bạn có thể tự set Authentication ở đây nếu muốn)
        chain.doFilter(req, res);
    }
}
