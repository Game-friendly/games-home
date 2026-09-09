package com.club.order.web;

import com.club.order.common.ApiResponse;
import com.club.order.domain.Order;
import com.club.order.domain.Review;
import com.club.order.repo.OrderRepository;
import com.club.order.service.ReviewService;
import com.club.order.web.dto.ReviewRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final OrderRepository orderRepo;

    /** 客户评价打手（订单完成后）。 */
    @PostMapping("/api/orders/{id}/review")
    public ApiResponse<Map<String, Object>> create(@PathVariable Long id,
                                                   @Valid @RequestBody ReviewRequest req,
                                                   @RequestAttribute("uid") Long uid) {
        Review r = reviewService.create(id, uid, req.toId(), req.rating(), req.comment());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("toId", r.getToId());
        m.put("rating", r.getRating());
        m.put("comment", r.getComment());
        return ApiResponse.ok(m);
    }

    @GetMapping("/api/orders/{id}/review")
    public ApiResponse<List<Map<String, Object>>> get(@PathVariable Long id) {
        List<Map<String, Object>> list = reviewService.byOrder(id).stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("orderId", r.getOrderId());
            m.put("fromId", r.getFromId());
            m.put("toId", r.getToId());
            m.put("rating", r.getRating());
            m.put("comment", r.getComment());
            m.put("createdAt", r.getCreatedAt());
            return m;
        }).toList();
        return ApiResponse.ok(list);
    }

    @GetMapping("/api/reviews/mine")
    public ApiResponse<List<Map<String, Object>>> mine(@RequestAttribute("uid") Long uid) {
        List<Map<String, Object>> list = reviewService.byWorker(uid).stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("orderId", r.getOrderId());
            m.put("orderTitle", orderRepo.findById(r.getOrderId()).map(Order::getTitle).orElse(""));
            m.put("rating", r.getRating());
            m.put("comment", r.getComment());
            m.put("createdAt", r.getCreatedAt());
            return m;
        }).toList();
        return ApiResponse.ok(list);
    }
}
