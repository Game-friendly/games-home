package com.club.order.service;

import com.club.order.common.BizException;
import com.club.order.domain.Earning;
import com.club.order.domain.Order;
import com.club.order.domain.OrderAssignment;
import com.club.order.repo.EarningRepository;
import com.club.order.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EarningService {

    private final EarningRepository earningRepo;
    private final OrderRepository orderRepo;

    /** 订单确认完成时入账。多人单把预算按实际接单人平均拆分，余数给第一位。 */
    @Transactional
    public void credit(Order order, List<OrderAssignment> activeAssignments) {
        if (activeAssignments.isEmpty()) return;
        int count = activeAssignments.size();
        int base = order.getBudget() / count;
        int remainder = order.getBudget() % count;
        for (int i = 0; i < activeAssignments.size(); i++) {
            OrderAssignment a = activeAssignments.get(i);
            if (earningRepo.findByOrderIdAndWorkerId(order.getId(), a.getWorkerId()).isPresent()) {
                continue; // 幂等
            }
            Earning e = new Earning();
            e.setOrderId(order.getId());
            e.setWorkerId(a.getWorkerId());
            e.setAmount(base + (i == 0 ? remainder : 0));
            earningRepo.save(e);
        }
    }

    public List<Map<String, Object>> mine(Long workerId) {
        return earningRepo.findByWorkerId(workerId, Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", e.getId());
                    m.put("orderId", e.getOrderId());
                    m.put("amount", e.getAmount());
                    m.put("status", e.getStatus().name());
                    m.put("settledAt", e.getSettledAt());
                    m.put("createdAt", e.getCreatedAt());
                    m.put("orderTitle", orderRepo.findById(e.getOrderId()).map(Order::getTitle).orElse(""));
                    return m;
                }).toList();
    }

    public List<Map<String, Object>> all() {
        return earningRepo.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", e.getId());
                    m.put("orderId", e.getOrderId());
                    m.put("workerId", e.getWorkerId());
                    m.put("amount", e.getAmount());
                    m.put("status", e.getStatus().name());
                    m.put("settledAt", e.getSettledAt());
                    m.put("createdAt", e.getCreatedAt());
                    return m;
                }).toList();
    }

    /** 管理员结算某订单的所有收益记录。 */
    @Transactional
    public int settle(Long orderId) {
        List<Earning> list = earningRepo.findByOrderId(orderId);
        if (list.isEmpty()) {
            throw new BizException(404, "该订单无收益记录");
        }
        int n = 0;
        for (Earning e : list) {
            if (e.getStatus() == Earning.EarningStatus.SETTLED) continue;
            e.setStatus(Earning.EarningStatus.SETTLED);
            e.setSettledAt(LocalDateTime.now());
            earningRepo.save(e);
            n++;
        }
        return n;
    }
}
