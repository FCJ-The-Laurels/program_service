package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity này cấu hình module nội dung nào sẽ được sử dụng cho mỗi lần phục hồi streak.
 * Ví dụ: Lần 1 dùng module A, lần 2 dùng module B.
 */
@Entity
@Table(name = "streak_recovery_configs", schema = "program")
@Getter
@Setter
public class StreakRecoveryConfig {
    /**
     * Lần thử phục hồi (1, 2, 3, ...). Đây là khóa chính.
     */
    @Id
    private Integer attemptOrder;

    /**
     * Mã của module nội dung (content module) sẽ được gán cho lần thử này.
     */
    @Column(nullable = false)
    private String moduleCode;
}
