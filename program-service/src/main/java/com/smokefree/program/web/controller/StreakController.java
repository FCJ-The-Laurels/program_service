package com.smokefree.program.web.controller;

import com.smokefree.program.domain.service.smoke.StreakService;
import com.smokefree.program.web.dto.streak.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/programs/{programId}/streak")
@RequiredArgsConstructor
public class StreakController {

    private final StreakService service;

    // Mở streak (nếu đã mở thì trả về streak hiện tại, không tạo thêm)
    @PostMapping("/start")
    @PreAuthorize("isAuthenticated()")
    public StreakView start(@PathVariable UUID programId,
                            @RequestBody(required = false) StartStreakReq req) {
        return service.start(programId, req == null ? null : req.startedAt());
    }

    // Đóng streak
    @PostMapping("/break")
    @PreAuthorize("isAuthenticated()")
    public StreakView breakStreak(@PathVariable UUID programId,
                                  @RequestBody BreakStreakReq req) {
        return service.breakStreak(programId, req.brokenAt(), req.smokeEventId(), req.note());
    }

    // Streak hiện tại
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public StreakView current(@PathVariable UUID programId) {
        return service.current(programId);
    }

    // Lịch sử streak
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public List<StreakView> history(@PathVariable UUID programId,
                                    @RequestParam(defaultValue = "20") int size) {
        return service.history(programId, size);
    }

    // Lịch sử break
    @GetMapping("/breaks")
    @PreAuthorize("isAuthenticated()")
    public List<StreakBreakRes> breaks(@PathVariable UUID programId,
                                       @RequestParam(defaultValue = "20") int size) {
        return service.breaks(programId, size);
    }
}
