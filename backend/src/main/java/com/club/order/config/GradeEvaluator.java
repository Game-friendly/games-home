package com.club.order.config;

import com.club.order.service.GradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GradeEvaluator {

    private final GradeService gradeService;

    @Scheduled(fixedDelay = 60_000)
    public void evaluate() {
        try {
            gradeService.evaluateAll();
        } catch (Exception ignored) {
        }
    }
}
