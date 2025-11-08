package com.smokefree.program.web.dto.quiz.admin;


import com.smokefree.program.domain.model.QuestionType;

public record AddQuestionReq(Integer orderNo, String questionText, QuestionType type, Integer points, String explanation) { }
