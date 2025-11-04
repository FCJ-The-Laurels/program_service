package com.smokefree.program.web.controller;


import com.smokefree.program.domain.service.smoke.StreakService;
import com.smokefree.program.web.dto.streak.StartStreakReq;
import com.smokefree.program.web.dto.streak.StreakView;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/programs/{programId}/streak")
@RequiredArgsConstructor
public class StreakController {

    private final StreakService service;

    @PostMapping("/start")
    public StreakView start(@PathVariable UUID programId, @RequestBody(required = false) StartStreakReq req) {
        return service.start(programId, req == null ? null : req.startedAt());
    }

    @GetMapping
    public StreakView current(@PathVariable UUID programId) {
        return service.current(programId);
    }
}
