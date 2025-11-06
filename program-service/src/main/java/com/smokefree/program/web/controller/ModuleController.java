// src/main/java/com/smokefree/program/web/controller/ModuleController.java
package com.smokefree.program.web.controller;


import com.smokefree.program.domain.service.ContentModuleService;
import com.smokefree.program.web.dto.plan.ContentModuleRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// src/main/java/com/smokefree/program/web/controller/ModuleController.java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/modules")
public class ModuleController {

    private final ContentModuleService service;

    @GetMapping("/{code}")
    public ResponseEntity<ContentModuleRes> getLatest(@PathVariable String code,
                                                      @RequestParam(defaultValue = "vi") String lang,
                                                      @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        var opt = service.getLatest(code, lang);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        var dto = opt.get();
        String inm = ifNoneMatch;
        if (inm != null) {
            inm = inm.trim();
            if (inm.startsWith("W/")) inm = inm.substring(2);
            if (inm.startsWith("\"") && inm.endsWith("\"") && inm.length() >= 2) {
                inm = inm.substring(1, inm.length() - 1);
            }
        }
        if (inm != null && inm.equals(dto.etag())) {
            return ResponseEntity.status(304).eTag(dto.etag()).build();
        }
        return ResponseEntity.ok().eTag(dto.etag()).body(dto);
    }

    @GetMapping("/{code}/v/{version}")
    public ResponseEntity<ContentModuleRes> getVersion(@PathVariable String code,
                                                       @PathVariable Integer version,
                                                       @RequestParam(defaultValue = "vi") String lang) {
        return service.getVersion(code, lang, version)
                .map(m -> ResponseEntity.ok().eTag(m.etag()).body(m))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

