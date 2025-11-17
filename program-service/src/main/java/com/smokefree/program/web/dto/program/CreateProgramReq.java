package com.smokefree.program.web.dto.program;

import java.util.UUID;

public record CreateProgramReq(
        Integer planDays,
        UUID coachId) {}