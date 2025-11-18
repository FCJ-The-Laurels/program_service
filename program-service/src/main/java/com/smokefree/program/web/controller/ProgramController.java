package com.smokefree.program.web.controller;

import com.smokefree.program.domain.service.ProgramService;
import com.smokefree.program.web.dto.program.*;
import com.smokefree.program.web.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/programs")
@RequiredArgsConstructor
public class ProgramController {
    private final ProgramService programService;

    @PostMapping
    public ProgramRes create(@RequestHeader("X-User-Id") UUID userId,
                             @RequestHeader(value="X-User-Tier", required=false) String tier,
                             @RequestBody CreateProgramReq req) {
        return programService.createProgram(userId, req, tier);
    }

    @GetMapping("/active")
    public ProgramRes getActive(@RequestHeader("X-User-Id") UUID userId,
                                @RequestHeader(value="X-Ent-State", required=false) String entState,
                                @RequestHeader(value="X-Ent-ExpiresAt", required=false) String entExp,
                                @RequestHeader(value="X-User-Tier", required=false) String tier) {
        var p = programService.getActive(userId).orElseThrow(() -> new NotFoundException("No ACTIVE program"));
        return programService.toRes(p, entState, entExp == null ? null : Instant.parse(entExp), tier);
    }
    @GetMapping
    public List<ProgramRes> listByUser(@RequestHeader("X-User-Id") UUID userId,
                                       @RequestHeader(value="X-User-Tier", required=false) String tier) {
        return programService.listByUser(userId).stream()
                .map(p -> programService.toRes(p, null, null, tier))
                .toList();
    }
}
