package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.SmokeEvent;
import com.smokefree.program.domain.model.SmokeEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface SmokeEventRepository extends JpaRepository<SmokeEvent, UUID> {
    List<SmokeEvent> findByProgramIdOrderByEventAtDesc(UUID programId);
    Optional<SmokeEvent> findTopByProgramIdAndEventTypeOrderByEventAtDesc(UUID programId, SmokeEventType type);
}
