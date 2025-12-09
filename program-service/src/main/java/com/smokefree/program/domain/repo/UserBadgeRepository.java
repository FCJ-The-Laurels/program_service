package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Thêm import Query
import java.util.List;
import java.util.UUID;

public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {
    boolean existsByUserIdAndBadgeIdAndProgramId(UUID userId, UUID badgeId, UUID programId);

    @Query("SELECT ub FROM UserBadge ub JOIN FETCH ub.badge WHERE ub.userId = :userId")
    List<UserBadge> findAllByUserIdWithBadge(UUID userId);
}
