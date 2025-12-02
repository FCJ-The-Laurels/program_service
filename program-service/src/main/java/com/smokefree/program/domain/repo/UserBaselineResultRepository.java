package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.UserBaselineResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserBaselineResultRepository extends JpaRepository<UserBaselineResult, UUID> {
    Optional<UserBaselineResult> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
    boolean existsByUserId(UUID userId);
}
