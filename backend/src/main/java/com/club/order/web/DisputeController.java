package com.club.order.web;

import com.club.order.common.ApiResponse;
import com.club.order.domain.Dispute;
import com.club.order.repo.DisputeRepository;
import com.club.order.service.DisputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;
    private final DisputeRepository disputeRepo;

    @PostMapping("/api/orders/{id}/dispute")
    public ApiResponse<Map<String, Object>> open(@PathVariable Long id,
                                                 @RequestBody Map<String, String> body,
                                                 @RequestAttribute("uid") Long uid) {
        Dispute d = disputeService.open(id, uid, body.getOrDefault("reason", ""));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("orderId", d.getOrderId());
        m.put("status", d.getStatus().name());
        return ApiResponse.ok(m);
    }

    @GetMapping("/api/orders/{id}/dispute")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long id) {
        Dispute d = disputeRepo.findByOrderIdAndStatus(id, Dispute.DisputeStatus.OPEN).orElse(null);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("open", d != null);
        if (d != null) {
            m.put("reason", d.getReason() == null ? "" : d.getReason());
            m.put("createdAt", d.getCreatedAt());
        }
        return ApiResponse.ok(m);
    }
}
