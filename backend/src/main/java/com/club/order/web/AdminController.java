package com.club.order.web;

import com.club.order.common.ApiResponse;
import com.club.order.common.BizException;
import com.club.order.domain.Dispute;
import com.club.order.domain.Earning;
import com.club.order.domain.User;
import com.club.order.domain.Withdrawal;
import com.club.order.service.AdminService;
import com.club.order.service.DashboardService;
import com.club.order.service.DisputeService;
import com.club.order.service.EarningService;
import com.club.order.service.OrderService;
import com.club.order.service.WithdrawalService;
import com.club.order.web.dto.OrderDTO;
import com.club.order.web.dto.UpdateOrderRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final OrderService orderService;
    private final AdminService adminService;
    private final EarningService earningService;
    private final DisputeService disputeService;
    private final WithdrawalService withdrawalService;
    private final DashboardService dashboardService;

    private void checkAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new BizException(403, "仅管理员可操作");
        }
    }

    @GetMapping("/orders")
    public ApiResponse<List<OrderDTO>> listAll(@RequestAttribute("role") String role) {
        checkAdmin(role);
        return ApiResponse.ok(adminService.listAll());
    }

    @PatchMapping("/orders/{id}")
    public ApiResponse<OrderDTO> update(@PathVariable Long id,
                                        @Valid @RequestBody UpdateOrderRequest req,
                                        @RequestAttribute("role") String role) {
        checkAdmin(role);
        return ApiResponse.ok(orderService.updateByAdmin(id, req));
    }

    @PostMapping("/orders/{id}/dispatch")
    public ApiResponse<OrderDTO> dispatch(@PathVariable Long id,
                                          @RequestBody Map<String, Long> body,
                                          @RequestAttribute("role") String role) {
        checkAdmin(role);
        return ApiResponse.ok(orderService.dispatch(id, body.get("workerId")));
    }

    @PostMapping("/workers/{id}/grade")
    public ApiResponse<Map<String, String>> setGrade(@PathVariable Long id,
                                                     @RequestBody Map<String, String> body,
                                                     @RequestAttribute("role") String role) {
        checkAdmin(role);
        User u = adminService.setWorkerGrade(id, body.get("grade"));
        return ApiResponse.ok(Map.of("workerId", String.valueOf(u.getId()), "grade", u.getGrade().name()));
    }

    @GetMapping("/workers")
    public ApiResponse<List<Map<String, Object>>> listWorkers(@RequestAttribute("role") String role) {
        checkAdmin(role);
        return ApiResponse.ok(adminService.listWorkers());
    }

    @PostMapping("/orders/{id}/settle")
    public ApiResponse<Map<String, String>> settle(@PathVariable Long id,
                                                   @RequestAttribute("role") String role) {
        checkAdmin(role);
        int count = earningService.settle(id);
        return ApiResponse.ok(Map.of("orderId", String.valueOf(id), "settled", String.valueOf(count)));
    }

    @GetMapping("/earnings")
    public ApiResponse<List<Map<String, Object>>> earnings(@RequestAttribute("role") String role) {
        checkAdmin(role);
        return ApiResponse.ok(earningService.all());
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard(@RequestAttribute("role") String role) {
        checkAdmin(role);
        return ApiResponse.ok(dashboardService.summary());
    }

    @GetMapping("/disputes")
    public ApiResponse<List<Map<String, Object>>> disputes(@RequestAttribute("role") String role) {
        checkAdmin(role);
        return ApiResponse.ok(disputeService.listAll().stream().map(d -> disputeMap(d)).toList());
    }

    @PostMapping("/disputes/{id}/resolve")
    public ApiResponse<Map<String, Object>> resolveDispute(@PathVariable Long id,
                                                           @RequestBody Map<String, String> body,
                                                           @RequestAttribute("role") String role) {
        checkAdmin(role);
        Dispute.DisputeOutcome outcome;
        try {
            outcome = Dispute.DisputeOutcome.valueOf(body.getOrDefault("outcome", "RELEASE"));
        } catch (IllegalArgumentException e) {
            throw new BizException(400, "裁决结果只能是 RELEASE/REFUND/CANCEL");
        }
        return ApiResponse.ok(disputeMap(disputeService.resolve(id, outcome, body.get("resolution"))));
    }

    @GetMapping("/withdrawals")
    public ApiResponse<List<Map<String, Object>>> withdrawals(@RequestAttribute("role") String role) {
        checkAdmin(role);
        return ApiResponse.ok(withdrawalService.listAll().stream().map(w -> withdrawalMap(w)).toList());
    }

    @PostMapping("/withdrawals/{id}/approve")
    public ApiResponse<Map<String, Object>> approveWithdrawal(@PathVariable Long id,
                                                              @RequestAttribute("role") String role) {
        checkAdmin(role);
        return ApiResponse.ok(withdrawalMap(withdrawalService.approve(id)));
    }

    @PostMapping("/withdrawals/{id}/reject")
    public ApiResponse<Map<String, Object>> rejectWithdrawal(@PathVariable Long id,
                                                             @RequestBody(required = false) Map<String, String> body,
                                                             @RequestAttribute("role") String role) {
        checkAdmin(role);
        String remark = body == null ? null : body.get("remark");
        return ApiResponse.ok(withdrawalMap(withdrawalService.reject(id, remark)));
    }

    private Map<String, Object> disputeMap(Dispute d) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("orderId", d.getOrderId());
        m.put("createdBy", d.getCreatedBy());
        m.put("reason", d.getReason() == null ? "" : d.getReason());
        m.put("status", d.getStatus().name());
        m.put("outcome", d.getOutcome() == null ? "" : d.getOutcome().name());
        m.put("resolution", d.getResolution() == null ? "" : d.getResolution());
        m.put("createdAt", d.getCreatedAt());
        m.put("resolvedAt", d.getResolvedAt());
        return m;
    }

    private Map<String, Object> withdrawalMap(Withdrawal w) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", w.getId());
        m.put("workerId", w.getWorkerId());
        m.put("amount", w.getAmount());
        m.put("status", w.getStatus().name());
        m.put("createdAt", w.getCreatedAt());
        m.put("handledAt", w.getHandledAt());
        m.put("remark", w.getRemark() == null ? "" : w.getRemark());
        return m;
    }
}
