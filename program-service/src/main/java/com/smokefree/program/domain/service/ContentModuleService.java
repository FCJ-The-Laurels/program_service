package com.smokefree.program.domain.service;



import com.smokefree.program.web.dto.plan.ContentModuleRes;

import java.util.Optional;

public interface ContentModuleService {
    Optional<ContentModuleRes> getLatest(String code, String lang);
    Optional<ContentModuleRes> getVersion(String code, String lang, Integer version);
}
