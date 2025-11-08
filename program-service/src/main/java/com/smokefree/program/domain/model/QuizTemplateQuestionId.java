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
public class QuizTemplateQuestionId implements Serializable {
    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "question_no", nullable = false)
    private Integer questionNo;
}
