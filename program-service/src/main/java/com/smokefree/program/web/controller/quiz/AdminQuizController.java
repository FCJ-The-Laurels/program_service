package com.smokefree.program.web.controller.quiz;

import com.smokefree.program.domain.model.QuizTemplate;
import com.smokefree.program.domain.service.quiz.AdminQuizService;
import com.smokefree.program.web.dto.quiz.admin.AddChoiceReq;
import com.smokefree.program.web.dto.quiz.admin.AddQuestionReq;
import com.smokefree.program.web.dto.quiz.template.CreateTemplateReq;
import com.smokefree.program.web.dto.quiz.template.UpdateTemplateReq;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/quizzes")
@RequiredArgsConstructor
public class AdminQuizController {

    private final AdminQuizService adminQuizService;

    @PostMapping
    public ResponseEntity<?> createTemplate(@RequestBody CreateTemplateReq req) {
        var tpl = adminQuizService.createTemplate(req.name());
        return ResponseEntity.ok(Map.of("id", tpl.getId(), "message", "Created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTemplate(@PathVariable UUID id, @RequestBody UpdateTemplateReq req) {
        var tpl = adminQuizService.updateTemplate(id, req.name(), req.version());
        return ResponseEntity.ok(Map.of("id", tpl.getId(), "message", "Updated successfully"));
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<?> archiveTemplate(@PathVariable UUID id) {
        adminQuizService.archiveTemplate(id);
        return ResponseEntity.ok(Map.of("message", "Archived successfully"));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publishTemplate(@PathVariable UUID id) {
        adminQuizService.publishTemplate(id);
        return ResponseEntity.ok(Map.of("message", "Published successfully"));
    }

    @PostMapping("/{id}/questions")
    public ResponseEntity<?> addQuestion(@PathVariable UUID id, @RequestBody AddQuestionReq req) {
        // Fix: text() -> questionText(), type() -> type().name()
        var qId = adminQuizService.addQuestion(id, req.orderNo(), req.questionText(), req.type().name(), req.points(), req.explanation());
        return ResponseEntity.ok(Map.of("questionId", qId, "message", "Question added"));
    }

    @PostMapping("/{id}/questions/{qNo}/choices")
    public ResponseEntity<?> addChoice(@PathVariable UUID id, @PathVariable Integer qNo, @RequestBody AddChoiceReq req) {
        // Fix: text() -> labelText(), correct() -> isCorrect()
        var cId = adminQuizService.addChoice(id, qNo, req.labelCode(), req.labelText(), req.isCorrect(), req.weight());
        return ResponseEntity.ok(Map.of("choiceId", cId, "message", "Choice added"));
    }
}
