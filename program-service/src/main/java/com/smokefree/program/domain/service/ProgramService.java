package com.smokefree.program.domain.service;

import com.smokefree.program.domain.model.Program;
import com.smokefree.program.web.dto.program.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramService {
    ProgramRes createProgram(UUID ownerUserId, CreateProgramReq req, String tierHeader);
    Optional<Program> getActive(UUID userId);
    ProgramRes toRes(Program p, String entState, Instant entExp, String tier);
    List<Program> listByUser(UUID userId);

}

