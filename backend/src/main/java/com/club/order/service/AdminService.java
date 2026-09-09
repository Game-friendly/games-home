package com.club.order.service;

import com.club.order.common.BizException;
import com.club.order.domain.*;
import com.club.order.repo.OrderAssignmentRepository;
import com.club.order.repo.OrderRepository;
import com.club.order.repo.UserRepository;
import com.club.order.web.dto.OrderDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final OrderRepository orderRepo;
    private final UserRepository userRepo;
    private final OrderAssignmentRepository assignmentRepo;

    public List<OrderDTO> listAll() {
        return orderRepo.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(this::dto).toList();
    }

    /** 打手列表（含等级与信用分），供后台管理。 */
    public List<Map<String, Object>> listWorkers() {
        return userRepo.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .filter(u -> u.getRole() != Role.CLIENT)
                .map(u -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("nickname", u.getNickname());
                    m.put("openid", u.getOpenid());
                    m.put("role", u.getRole().name());
                    m.put("grade", u.getGrade().name());
                    m.put("creditScore", u.getCreditScore());
                    return m;
                }).toList();
    }

    @Transactional
    public User setWorkerGrade(Long workerId, String gradeStr) {
        User u = userRepo.findById(workerId).orElseThrow(() -> new BizException(404, "用户不存在"));
        Grade grade;
        try {
            grade = Grade.valueOf(gradeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(400, "等级只能是 A/B/C");
        }
        u.setRole(Role.WORKER);
        u.setGrade(grade);
        return userRepo.save(u);
    }

    private OrderDTO dto(Order o) {
        List<OrderAssignment> assignments = assignmentRepo.findByOrderIdOrderByIdAsc(o.getId());
        Map<Long, String> grades = new java.util.LinkedHashMap<>();
        for (OrderAssignment a : assignments) {
            if (!grades.containsKey(a.getWorkerId())) {
                userRepo.findById(a.getWorkerId()).ifPresent(u -> grades.put(u.getId(), u.getGrade().name()));
            }
        }
        return OrderDTO.of(o, assignments, grades, true);
    }
}
