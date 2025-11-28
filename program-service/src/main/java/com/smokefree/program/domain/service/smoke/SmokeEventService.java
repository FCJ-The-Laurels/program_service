package com.smokefree.program.domain.service.smoke;

import com.smokefree.program.domain.model.SmokeEvent;
import com.smokefree.program.web.dto.smoke.CreateSmokeEventReq;
import com.smokefree.program.web.dto.smoke.SmokeEventStatisticsRes;

import java.util.List;
import java.util.UUID;

public interface SmokeEventService {
    SmokeEvent create(UUID programId, CreateSmokeEventReq req);
    List<SmokeEvent> getHistory(UUID programId, int size);
    SmokeEventStatisticsRes getStatistics(UUID programId, String period);
}

