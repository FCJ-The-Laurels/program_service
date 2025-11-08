package com.smokefree.program.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class QuizChoiceLabelId implements Serializable {
    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "question_no", nullable = false)
    private Integer questionNo;

    @Column(name = "label_code", length = 8, nullable = false)
    private String labelCode; // A, B, C... hoặc bất kỳ mã nào bạn dùng
}
