package com.smokefree.program.domain.service.onboarding;

import com.smokefree.program.domain.model.SeverityLevel;
import com.smokefree.program.domain.model.UserBaselineResult;
import com.smokefree.program.domain.repo.UserBaselineResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BaselineResultServiceImpl implements BaselineResultService {

    private final UserBaselineResultRepository repo;

    @Override
    @Transactional
    public UserBaselineResult saveOrUpdate(UUID userId, UUID quizTemplateId, int totalScore, SeverityLevel severity) {
        UserBaselineResult existing = repo.findFirstByUserIdOrderByCreatedAtDesc(userId).orElse(null);
        if (existing == null) {
            UserBaselineResult r = new UserBaselineResult();
            r.setUserId(userId);
            r.setQuizTemplateId(quizTemplateId);
            r.setTotalScore(totalScore);
            r.setSeverity(severity);
            return repo.save(r);
        }
        existing.setQuizTemplateId(quizTemplateId);
        existing.setTotalScore(totalScore);
        existing.setSeverity(severity);
        return repo.save(existing);
    }

    @Override
    public Optional<UserBaselineResult> latest(UUID userId) {
        return repo.findFirstByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public boolean hasBaseline(UUID userId) {
        return repo.existsByUserId(userId);
    }
}
