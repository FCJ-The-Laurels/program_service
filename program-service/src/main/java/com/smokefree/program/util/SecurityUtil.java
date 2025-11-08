package com.smokefree.program.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.stream.Collectors;

public final class SecurityUtil {
    private SecurityUtil() {}

    // --- USER ID ---
    public static UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return null;
        Object p = auth.getPrincipal();
        if (p == null) return null;
        if (p instanceof UUID u) return u;
        try {
            var m = p.getClass().getMethod("getId");
            Object id = m.invoke(p);
            if (id instanceof UUID u) return u;
            if (id instanceof String s) return UUID.fromString(s);
        } catch (Exception ignore) {}
        if (p instanceof String s) {
            try { return UUID.fromString(s); } catch (IllegalArgumentException ignore) {}
        }
        return null;
    }

    public static UUID requireUserId() {
        UUID id = currentUserId();
        if (id == null) throw new IllegalStateException("Missing authenticated user");
        return id;
    }

    // --- ROLES ---
    public static Set<String> currentRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Set.of();
        return auth.getAuthorities().stream()
                .map(a -> a == null ? null : a.getAuthority())
                .filter(Objects::nonNull)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .collect(Collectors.toSet());
    }
    public static boolean hasRole(String role) {
        return currentRoles().contains(role);
    }
    public static boolean hasAnyRole(String... roles) {
        Set<String> cur = currentRoles();
        for (String r : roles) if (cur.contains(r)) return true;
        return false;
    }

    // --- VIP ---
    public static boolean isVip(UUID programId) {
        if (programId == null) return false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        Set<UUID> vip = extractVipPrograms(auth.getDetails());
        return vip.contains(programId);
    }

    @SuppressWarnings("unchecked")
    private static Set<UUID> extractVipPrograms(Object details) {
        if (details instanceof Map<?, ?> m) {
            Map<String, Object> map = (Map<String, Object>) m;
            Object val = map.getOrDefault("vip_programs", map.get("X-Vip-Programs"));
            return toUuidSet(val);
        }

        // details là chuỗi CSV hoặc một collection
        if (details instanceof String s) return parseCsvUUIDs(s);
        if (details instanceof Collection<?> c) return toUuidSet(c);
        return Set.of();
    }

    private static Set<UUID> toUuidSet(Object val) {
        if (val == null) return Set.of();
        if (val instanceof UUID u) return Set.of(u);
        if (val instanceof String s) return parseCsvUUIDs(s);
        if (val instanceof Collection<?> c) {
            return c.stream().map(SecurityUtil::coerceUUID)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }

    private static Set<UUID> parseCsvUUIDs(String s) {
        if (s == null || s.isBlank()) return Set.of();
        return Arrays.stream(s.split("[,;\\s]+"))
                .map(SecurityUtil::coerceUUID)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static UUID coerceUUID(Object o) {
        if (o instanceof UUID u) return u;
        if (o instanceof String s) {
            try { return UUID.fromString(s.trim()); } catch (Exception ignore) {}
        }
        return null;
    }
}
