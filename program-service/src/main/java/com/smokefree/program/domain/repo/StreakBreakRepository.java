package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.StreakBreak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface StreakBreakRepository extends JpaRepository<StreakBreak, UUID> {
    List<StreakBreak> findByStreakIdOrderByBrokenAtDesc(UUID streakId);
    // (tuỳ chọn) nếu muốn query theo program:
    List<StreakBreak> findByProgramIdOrderByBrokenAtDesc(UUID programId);
}

