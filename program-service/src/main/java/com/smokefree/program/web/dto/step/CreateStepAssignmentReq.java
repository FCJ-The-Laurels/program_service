package com.smokefree.program.web.dto.step;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateStepAssignmentReq(
        @NotNull @Min(1) Integer stepNo,
        @NotNull @Min(1) Integer plannedDay,
        String note
) {}
