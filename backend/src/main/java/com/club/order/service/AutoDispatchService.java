package com.club.order.service;

import com.club.order.domain.AssignmentStatus;
import com.club.order.domain.Grade;
import com.club.order.domain.Order;
import com.club.order.domain.OrderStatus;
import com.club.order.domain.Role;
import com.club.order.domain.User;
import com.club.order.repo.OrderAssignmentRepository;
import com.club.order.repo.OrderRepository;
import com.club.order.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AutoDispatchService {

    private static final List<AssignmentStatus> ACTIVE = List.of(
            AssignmentStatus.RESERVED, AssignmentStatus.ACCEPTED);

    private final OrderRepository orderRepo;
    private final OrderAssignmentRepository assignmentRepo;
    private final UserRepository userRepo;
    private final OrderService orderService;

    @Value("${app.auto-dispatch-enabled:false}")
    private boolean enabled;

    @Value("${app.auto-dispatch-after-minutes:30}")
    private int afterMinutes;

    @Value("${app.min-accept-credit:60}")
    private int minAcceptCredit;

    public int dispatchAll() {
        if (!enabled) return 0;
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(afterMinutes);
        List<Order> candidates = orderRepo.findByStatus(OrderStatus.PUBLISHED, Sort.by(Sort.Direction.ASC, "id"));
        List<User> workers = userRepo.findAll().stream()
                .filter(u -> u.getRole() == Role.WORKER)
                .filter(u -> u.getCreditScore() >= minAcceptCredit)
                .sorted(Comparator.comparingInt((User u) -> u.getGrade().rank).reversed()
                        .thenComparingLong(User::getId))
                .toList();

        Set<Long> used = new HashSet<>();
        int dispatched = 0;
        for (Order o : candidates) {
            if (o.getCreatedAt() == null || o.getCreatedAt().isAfter(threshold)) continue;
            int filled = assignmentRepo.findByOrderIdAndStatusIn(o.getId(), ACTIVE).size();
            if (filled >= o.getRequiredWorkers()) continue;
            for (User w : workers) {
                if (filled >= o.getRequiredWorkers()) break;
                if (used.contains(w.getId())) continue;
                orderService.dispatch(o.getId(), w.getId());
                used.add(w.getId());
                filled++;
                dispatched++;
            }
        }
        return dispatched;
    }
}
