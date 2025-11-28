package com.smokefree.program.web.dto.quiz.template;

public record UpdateTemplateReq(
    String name,
    Integer version
) {}