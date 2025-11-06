// src/main/java/com/smokefree/program/domain/repo/ContentModuleRepo.java
package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.ContentModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentModuleRepo extends JpaRepository<ContentModule, UUID> {
    Optional<ContentModule> findFirstByCodeAndLangOrderByVersionDesc(String code, String lang);
    Optional<ContentModule> findByCodeAndLangAndVersion(String code, String lang, Integer version);
    List<ContentModule> findByCodeAndLangOrderByVersionDesc(String code, String lang);
}
