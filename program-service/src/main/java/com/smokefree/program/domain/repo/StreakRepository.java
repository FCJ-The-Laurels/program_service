package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.Streak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface StreakRepository extends JpaRepository<Streak, UUID> {
    Optional<Streak> findFirstByProgramIdAndEndedAtIsNullOrderByStartedAtDesc(UUID programId);
    Optional<Streak> findTopByProgramIdOrderByStartedAtDesc(UUID programId);
}
