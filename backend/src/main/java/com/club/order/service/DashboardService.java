package com.club.order.service;

import com.club.order.domain.Dispute;
import com.club.order.domain.Earning;
import com.club.order.domain.Order;
import com.club.order.domain.OrderStatus;
import com.club.order.domain.Role;
import com.club.order.domain.User;
import com.club.order.repo.DisputeRepository;
import com.club.order.repo.EarningRepository;
import com.club.order.repo.OrderRepository;
import com.club.order.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepo;
    private final UserRepository userRepo;
    private final EarningRepository earningRepo;
    private final DisputeRepository disputeRepo;

    public Map<String, Object> summary() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalOrders", orderRepo.count());
        m.put("publishedOrders", orderRepo.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PUBLISHED).count());
        m.put("completedOrders", orderRepo.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED).count());
        m.put("disputedOrders", orderRepo.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.DISPUTED).count());
        m.put("openDisputes", disputeRepo.findAllByOrderByIdDesc().stream()
                .filter(d -> d.getStatus() == Dispute.DisputeStatus.OPEN).count());
        m.put("workers", userRepo.findAll().stream()
                .filter(u -> u.getRole() == Role.WORKER).count());
        m.put("clients", userRepo.findAll().stream()
                .filter(u -> u.getRole() == Role.CLIENT).count());
        m.put("pendingEarnings", earningRepo.findAll().stream()
                .filter(e -> e.getStatus() == Earning.EarningStatus.PENDING)
                .mapToLong(Earning::getAmount).sum());
        m.put("settledEarnings", earningRepo.findAll().stream()
                .filter(e -> e.getStatus() == Earning.EarningStatus.SETTLED)
                .mapToLong(Earning::getAmount).sum());
        return m;
    }
}
