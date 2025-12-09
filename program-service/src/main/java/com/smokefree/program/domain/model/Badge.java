package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity định nghĩa thông tin Huy hiệu.
 * (Danh mục các loại huy hiệu có trong hệ thống)
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "badges", schema = "program")
public class Badge {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code; // PROG_LV1, STREAK_LV2...

    @Column(nullable = false)
    private String category; // PROGRAM, STREAK, QUIZ

    @Column(nullable = false)
    private Integer level; // 1, 2, 3

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "created_at")
    private Instant createdAt;
}
