package com.smokefree.program.web.dto.quiz.coach;

import java.util.UUID;
import java.time.OffsetDateTime;

public record CloneForVipRes(
        UUID newTemplateId, UUID assignmentId, OffsetDateTime dueAt) { }
