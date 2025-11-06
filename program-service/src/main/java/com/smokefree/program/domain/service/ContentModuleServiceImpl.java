package com.smokefree.program.domain.service;

import com.smokefree.program.domain.repo.ContentModuleRepo;

import com.smokefree.program.web.dto.plan.ContentModuleRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class ContentModuleServiceImpl implements ContentModuleService {

    private final ContentModuleRepo repo;

    @Override
    public java.util.Optional<ContentModuleRes> getLatest(String code, String lang) {
        return repo.findFirstByCodeAndLangOrderByVersionDesc(code, normalize(lang))
                .map(m -> new ContentModuleRes(
                        m.getId(), m.getCode(), m.getType(), m.getLang(), m.getVersion(),
                        m.getPayload(), m.getUpdatedAt(),
                        etag(m.getCode(), m.getLang(), m.getVersion())
                ));
    }

    @Override
    public java.util.Optional<ContentModuleRes> getVersion(String code, String lang, Integer version) {
        return repo.findByCodeAndLangAndVersion(code, normalize(lang), version)
                .map(m -> new ContentModuleRes(
                        m.getId(), m.getCode(), m.getType(), m.getLang(), m.getVersion(),
                        m.getPayload(), m.getUpdatedAt(),
                        etag(m.getCode(), m.getLang(), m.getVersion())
                ));
    }

    private static String normalize(String lang) { return (lang == null || lang.isBlank()) ? "vi" : lang; }
    private static String etag(String code, String lang, Integer ver) { return code + ":" + lang + ":v" + ver; }
}
