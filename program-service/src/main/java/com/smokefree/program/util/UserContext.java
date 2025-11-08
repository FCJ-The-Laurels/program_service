package com.smokefree.program.util;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;

public record UserContext(
        UUID userId,
        Set<String> roles,
        Set<UUID> vipPrograms,
        long iat, long exp
) implements Principal {
    @Override public String getName() { return userId.toString(); }
    public boolean hasRole(String r) { return roles != null && roles.contains(r); }
    public boolean isVip(UUID programId) { return vipPrograms != null && vipPrograms.contains(programId); }
}
