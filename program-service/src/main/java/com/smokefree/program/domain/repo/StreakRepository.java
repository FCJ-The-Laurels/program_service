package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.Streak;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface StreakRepository extends JpaRepository<Streak, UUID> {
    Optional<Streak> findFirstByProgramIdAndEndedAtIsNullOrderByStartedAtDesc(UUID programId);
    List<Streak> findByProgramIdOrderByStartedAtDesc(UUID programId, Pageable pageable);
    void deleteAllByProgramId(UUID programId);
}
