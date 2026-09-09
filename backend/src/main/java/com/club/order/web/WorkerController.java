package com.club.order.web;

import com.club.order.common.ApiResponse;
import com.club.order.domain.Review;
import com.club.order.domain.Role;
import com.club.order.domain.User;
import com.club.order.repo.OrderAssignmentRepository;
import com.club.order.repo.OrderRepository;
import com.club.order.repo.ReviewRepository;
import com.club.order.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 首页大神榜：按等级和信用分排序，附带胜率、好评率、接单数、起价。 */
@RestController
@RequiredArgsConstructor
public class WorkerController {

    private final UserRepository userRepo;
    private final OrderAssignmentRepository assignmentRepo;
    private final ReviewRepository reviewRepo;
    private final OrderRepository orderRepo;

    @GetMapping("/api/workers/top")
    public ApiResponse<List<Map<String, Object>>> top() {
        List<User> workers = userRepo.findAll().stream()
                .filter(u -> u.getRole() == Role.WORKER)
                .toList();

        List<Map<String, Object>> list = workers.stream().map(w -> {
            long completed = assignmentRepo.findByWorkerId(w.getId()).stream()
                    .filter(a -> a.getSubmittedAt() != null)
                    .count();
            List<Review> reviews = reviewRepo.findByToId(w.getId());
            double rating = reviews.isEmpty()
                    ? 0.0
                    : reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", w.getId());
            m.put("nickname", w.getNickname());
            m.put("grade", w.getGrade().name());
            m.put("creditScore", w.getCreditScore());
            m.put("winRate", w.getWinRate());
            m.put("minPrice", w.getMinPrice());
            m.put("completed", completed);
            m.put("rating", Math.round(rating * 10) / 10.0);
            m.put("avatar", w.getAvatar());
            return m;
        }).sorted(Comparator
                .comparingInt((Map<String, Object> m) -> gradeWeight((String) m.get("grade")))
                .thenComparingInt(m -> (Integer) m.get("creditScore"))
                .reversed())
                .toList();

        return ApiResponse.ok(list);
    }

    @GetMapping("/api/workers/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        User w = userRepo.findById(id).orElseThrow(() -> new com.club.order.common.BizException(404, "接单人不存在"));
        if (w.getRole() != Role.WORKER) {
            throw new com.club.order.common.BizException(400, "该用户不是接单人");
        }
        Map<String, Object> m = stats(w);
        List<Map<String, Object>> reviews = reviewRepo.findByToId(id).stream().map(r -> {
            Map<String, Object> rmap = new LinkedHashMap<>();
            rmap.put("id", r.getId());
            rmap.put("orderId", r.getOrderId());
            rmap.put("orderTitle", orderRepo.findById(r.getOrderId()).map(com.club.order.domain.Order::getTitle).orElse(""));
            rmap.put("rating", r.getRating());
            rmap.put("comment", r.getComment());
            rmap.put("createdAt", r.getCreatedAt());
            return rmap;
        }).toList();
        m.put("reviews", reviews);
        return ApiResponse.ok(m);
    }

    private Map<String, Object> stats(User w) {
        long completed = assignmentRepo.findByWorkerId(w.getId()).stream()
                .filter(a -> a.getSubmittedAt() != null)
                .count();
        List<Review> reviews = reviewRepo.findByToId(w.getId());
        double rating = reviews.isEmpty()
                ? 0.0
                : reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", w.getId());
        m.put("nickname", w.getNickname());
        m.put("grade", w.getGrade().name());
        m.put("creditScore", w.getCreditScore());
        m.put("winRate", w.getWinRate());
        m.put("minPrice", w.getMinPrice());
        m.put("completed", completed);
        m.put("rating", Math.round(rating * 10) / 10.0);
        m.put("avatar", w.getAvatar());
        return m;
    }

    private int gradeWeight(String grade) {
        return switch (grade) {
            case "A" -> 3;
            case "B" -> 2;
            default -> 1;
        };
    }
}
