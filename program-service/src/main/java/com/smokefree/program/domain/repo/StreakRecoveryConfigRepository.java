package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.StreakRecoveryConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface StreakRecoveryConfigRepository extends JpaRepository<StreakRecoveryConfig, UUID> {
    Optional<StreakRecoveryConfig> findByAttemptOrder(int attemptOrder);
}
