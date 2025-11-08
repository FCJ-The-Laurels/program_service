package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quiz_choice_labels", schema = "program")
@Getter @Setter
public class QuizChoiceLabel {

    @EmbeddedId
    private QuizChoiceLabelId id;

    // Liên kết về câu hỏi: dùng 2 cột (template_id, question_no) từ PK của label
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "template_id", referencedColumnName = "template_id",
                    insertable = false, updatable = false),
            @JoinColumn(name = "question_no", referencedColumnName = "question_no",
                    insertable = false, updatable = false)
    })
    private QuizTemplateQuestion question;

    @Column(name = "label_text", nullable = false)
    private String labelText;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "weight")
    private Integer weight;

    /** Tiện ích: khi set question thì đồng bộ phần PK tương ứng */
    public void setQuestion(QuizTemplateQuestion q) {
        this.question = q;
        if (q != null) {
            if (this.id == null) this.id = new QuizChoiceLabelId();
            this.id.setTemplateId(q.getId().getTemplateId());
            this.id.setQuestionNo(q.getId().getQuestionNo());
        }
    }
}
