package com.smokefree.program.domain.service.smoke;

import com.smokefree.program.domain.model.SmokeEvent;
import com.smokefree.program.web.dto.smoke.CreateSmokeEventReq;


import java.util.UUID;

public interface SmokeEventService {
    SmokeEvent create(UUID programId, CreateSmokeEventReq req);
}
