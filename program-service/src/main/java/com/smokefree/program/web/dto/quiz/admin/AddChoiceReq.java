package com.smokefree.program.web.dto.quiz.admin;

public record AddChoiceReq(String labelCode, String labelText, Boolean isCorrect, Integer weight) { }
