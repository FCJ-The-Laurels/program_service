//package com.smokefree.program.web.controller.quiz;
//
//import com.smokefree.program.domain.service.QuizService;
//import com.smokefree.program.web.dto.quiz.QuizAnswerReq;
//import com.smokefree.program.web.dto.quiz.QuizAnswerRes;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//
///**
// * QuizOnboardingController - Endpoint cho baseline quiz (onboarding)
// *
// * Endpoints:
// * - POST /api/v1/quiz/baseline - Submit baseline quiz answers
// * - GET /api/v1/quiz/severity/{score} - Map score to severity level
// */
//@Slf4j
//@RestController
//@RequestMapping("/api/v1/quiz")
//@RequiredArgsConstructor
//public class QuizOnboardingController {
//
//    private final QuizService quizService;
//
//    /**
//     * POST /api/v1/quiz/baseline
//     * Submit baseline quiz answers để tính mức độ lệ thuộc nicotine
//     *
//     * Request Body:
//     * {
//     *   "answers": [
//     *     { "q": 1, "score": 3 },
//     *     { "q": 2, "score": 2 },
//     *     { "q": 3, "score": 1 },
//     *     ...
//     *   ]
//     * }
//     *
//     * Headers:
//     * - X-User-Id (optional): UUID của user (for tracking)
//     * - X-User-Tier (optional): BASIC, PREMIUM, VIP
//     *
//     * Response:
//     * {
//     *   "success": true,
//     *   "data": {
//     *     "total": 25,
//     *     "severity": "MODERATE",
//     *     "planDays": 60,
//     *     "recommendation": {
//     *       "tier": "premium",
//     *       "alternatives": ["vip"],
//     *       "reason": "Your nicotine dependence is moderate..."
//     *     }
//     *   }
//     * }
//     */
//    @PostMapping("/baseline")
//    public ResponseEntity<Map<String, Object>> submitBaseline(
//            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
//            @RequestHeader(value = "X-User-Name", required = false) String userName,
//            @RequestHeader(value = "X-User-Tier", required = false) String userTier,
//            @RequestBody QuizAnswerReq request) {
//
//        log.info("[OnboardingQuiz] submitBaseline - userId: {}, answers: {}", userId, request.answers().size());
//
//        try {
//            QuizAnswerRes result = quizService.submitAnswers(userId, request, userTier);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
//            response.put("data", result);
//            response.put("message", "Baseline quiz assessed successfully");
//            response.put("timestamp", System.currentTimeMillis());
//
//            return ResponseEntity.ok(response);
//        } catch (IllegalArgumentException e) {
//            log.warn("[OnboardingQuiz] Validation error: {}", e.getMessage());
//            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
//        } catch (Exception e) {
//            log.error("[OnboardingQuiz] Error processing baseline quiz", e);
//            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
//        }
//    }
//
//    /**
//     * GET /api/v1/quiz/severity/{score}
//     * Map a score to severity level
//     *
//     * Path Variables:
//     * - score: Integer score value
//     *
//     * Response:
//     * {
//     *   "success": true,
//     *   "data": {
//     *     "score": 25,
//     *     "severity": "MODERATE",
//     *     "planDays": 60,
//     *     "recommendedTier": "premium"
//     *   }
//     * }
//     */
//    @GetMapping("/severity/{score}")
//    public ResponseEntity<Map<String, Object>> mapSeverity(@PathVariable int score) {
//
//        log.info("[OnboardingQuiz] mapSeverity - score: {}", score);
//
//        try {
//            var severity = quizService.mapSeverity(score);
//            int planDays = quizService.recommendPlanDays(severity);
//
//            Map<String, Object> data = new HashMap<>();
//            data.put("score", score);
//            data.put("severity", severity.name());
//            data.put("planDays", planDays);
//            data.put("recommendedTier", mapSeverityToTier(severity));
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
//            response.put("data", data);
//            response.put("message", "Severity level calculated");
//
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            log.error("[OnboardingQuiz] Error mapping severity", e);
//            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
//        }
//    }
//
//    /**
//     * Utility: Map severity level to recommended tier
//     */
//    private String mapSeverityToTier(com.smokefree.program.domain.model.SeverityLevel severity) {
//        return switch (severity) {
//            case LOW -> "basic";
//            case MODERATE -> "premium";
//            case HIGH -> "vip";
//        };
//    }
//
//    /**
//     * Utility method to build error response
//     */
//    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
//        Map<String, Object> errorResponse = new HashMap<>();
//        errorResponse.put("success", false);
//        errorResponse.put("error", status.getReasonPhrase());
//        errorResponse.put("message", message);
//        errorResponse.put("status", status.value());
//        errorResponse.put("timestamp", System.currentTimeMillis());
//        return ResponseEntity.status(status).body(errorResponse);
//    }
//}
//
