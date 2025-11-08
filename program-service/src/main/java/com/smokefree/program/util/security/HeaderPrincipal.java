// util/security/HeaderPrincipal.java
package com.smokefree.program.util.security;

import lombok.Getter;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;

@Getter
public class HeaderPrincipal implements Principal {
    private final UUID id;
    private final Set<String> roles;       // ví dụ: ADMIN, COACH, CUSTOMER
    private final Set<UUID> vipPrograms;   // các program mà user là VIP

    public HeaderPrincipal(UUID id, Set<String> roles, Set<UUID> vipPrograms) {
        this.id = id;
        this.roles = roles;
        this.vipPrograms = vipPrograms;
    }

    // SecurityUtil.currentUserId() đang dùng reflection getId()
    public UUID getId() { return id; }

    @Override public String getName() { return id.toString(); }

    public boolean hasRole(String r) { return roles != null && roles.contains(r); }
    public boolean isVip(UUID programId) { return vipPrograms != null && vipPrograms.contains(programId); }
}
