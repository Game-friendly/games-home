package com.club.order.service;

import com.club.order.domain.Grade;
import com.club.order.domain.OrderAssignment;
import com.club.order.domain.Review;
import com.club.order.domain.Role;
import com.club.order.domain.User;
import com.club.order.repo.OrderAssignmentRepository;
import com.club.order.repo.ReviewRepository;
import com.club.order.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final UserRepository userRepo;
    private final OrderAssignmentRepository assignmentRepo;
    private final ReviewRepository reviewRepo;

    @Value("${app.grade-up-min-completed:10}")
    private int minCompleted;

    @Value("${app.grade-up-min-rating:4.5}")
    private double minRating;

    @Value("${app.grade-up-min-credit:80}")
    private int minCredit;

    @Value("${app.grade-down-max-credit:40}")
    private int maxCreditForDowngrade;

    @Transactional
    public int evaluateAll() {
        int changed = 0;
        for (User u : userRepo.findAll()) {
            if (u.getRole() != Role.WORKER) continue;
            Grade before = u.getGrade();
            if (maybeUpgrade(u)) {
                u.setGrade(next(u.getGrade()));
                userRepo.save(u);
                changed++;
                continue;
            }
            if (u.getGrade() != Grade.C && u.getCreditScore() < maxCreditForDowngrade) {
                u.setGrade(previous(u.getGrade()));
                userRepo.save(u);
                changed++;
            }
        }
        return changed;
    }

    private boolean maybeUpgrade(User u) {
        if (u.getGrade() == Grade.A || u.getCreditScore() < minCredit) return false;
        List<OrderAssignment> all = assignmentRepo.findByWorkerId(u.getId());
        long completed = all.stream().filter(a -> a.getSubmittedAt() != null).count();
        if (completed < minCompleted) return false;
        List<Review> reviews = reviewRepo.findByToId(u.getId());
        if (reviews.isEmpty()) return false;
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
        return avg >= minRating;
    }

    private Grade next(Grade g) {
        return switch (g) {
            case C -> Grade.B;
            case B -> Grade.A;
            case A -> Grade.A;
        };
    }

    private Grade previous(Grade g) {
        return switch (g) {
            case A -> Grade.B;
            case B -> Grade.C;
            case C -> Grade.C;
        };
    }
}
