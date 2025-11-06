package com.smokefree.program.web.dto.plan;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record ContentModuleRes(
        UUID id,
        String code,
        String type,
        String lang,
        Integer version,
        Map<String,Object> payload,
        OffsetDateTime updatedAt,
        String etag
) {}
