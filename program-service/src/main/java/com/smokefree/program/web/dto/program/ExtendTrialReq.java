package com.smokefree.program.web.dto.program;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ExtendTrialReq(
        @NotNull(message = "additionalDays is required")
        @Min(value = 1, message = "additionalDays must be >= 1")
        Integer additionalDays
) {}

