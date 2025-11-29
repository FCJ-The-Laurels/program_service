package com.smokefree.program.web.controller.quiz;

import com.smokefree.program.domain.service.quiz.AdminQuizService;
import com.smokefree.program.web.dto.quiz.admin.CreateFullQuizReq;
import com.smokefree.program.web.dto.quiz.admin.UpdateFullQuizReq;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/quizzes")
@RequiredArgsConstructor
public class AdminQuizController {

    private final AdminQuizService adminQuizService;

    /**
     * API tối ưu: Tạo một Quiz Template hoàn chỉnh với đầy đủ câu hỏi và lựa chọn.
     */
    @PostMapping
    public ResponseEntity<?> createFullQuiz(@Valid @RequestBody CreateFullQuizReq req) {
        var tpl = adminQuizService.createFullQuiz(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", tpl.getId(), "message", "Quiz '" + tpl.getName() + "' created successfully."));
    }

    /**
     * API tối ưu: Cập nhật toàn bộ nội dung của một Quiz Template (trạng thái DRAFT).
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateFullQuiz(@PathVariable UUID id, @Valid @RequestBody UpdateFullQuizReq req) {
        adminQuizService.updateFullQuiz(id, req);
        return ResponseEntity.ok(Map.of("message", "Quiz template updated successfully with " + req.questions().size() + " questions."));
    }

    /**
     * Chuyển trạng thái template thành PUBLISHED.
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publishTemplate(@PathVariable UUID id) {
        adminQuizService.publishTemplate(id);
        return ResponseEntity.ok(Map.of("message", "Published successfully"));
    }

    /**
     * Chuyển trạng thái template thành ARCHIVED.
     */
    @PutMapping("/{id}/archive")
    public ResponseEntity<?> archiveTemplate(@PathVariable UUID id) {
        adminQuizService.archiveTemplate(id);
        return ResponseEntity.ok(Map.of("message", "Archived successfully"));
    }
}
