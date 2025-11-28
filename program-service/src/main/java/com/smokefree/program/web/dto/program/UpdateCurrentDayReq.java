package com.smokefree.program.web.dto.program;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCurrentDayReq(
        @NotNull(message = "currentDay is required")
        @Min(value = 1, message = "currentDay must be >= 1")
        Integer currentDay
) {}

