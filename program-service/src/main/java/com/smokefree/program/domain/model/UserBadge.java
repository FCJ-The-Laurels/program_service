package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity lưu trữ huy hiệu mà người dùng đã đạt được.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "user_badges", schema = "program")
public class UserBadge {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "badge_id", nullable = false)
    private UUID badgeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", insertable = false, updatable = false)
    private Badge badge;

    @Column(name = "program_id")
    private UUID programId;

    @Column(name = "earned_at")
    private Instant earnedAt;

    @PrePersist
    void preInsert() {
        if (earnedAt == null) {
            earnedAt = Instant.now();
        }
    }
}
