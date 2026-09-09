package com.club.order.web;

import com.club.order.common.ApiResponse;
import com.club.order.common.BizException;
import com.club.order.domain.Withdrawal;
import com.club.order.service.WithdrawalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/earnings")
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    @GetMapping("/withdrawable")
    public ApiResponse<Map<String, Object>> withdrawable(@RequestAttribute("uid") Long uid) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("amount", withdrawalService.withdrawable(uid));
        return ApiResponse.ok(m);
    }

    @GetMapping("/withdrawals")
    public ApiResponse<List<Map<String, Object>>> mine(@RequestAttribute("uid") Long uid) {
        return ApiResponse.ok(withdrawalService.mine(uid).stream().map(this::map).toList());
    }

    @PostMapping("/withdrawals")
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body,
                                                   @RequestAttribute("uid") Long uid) {
        Object raw = body.get("amount");
        if (!(raw instanceof Number)) {
            throw new BizException(400, "提现金额不能为空");
        }
        int amount = ((Number) raw).intValue();
        return ApiResponse.ok(map(withdrawalService.create(uid, amount)));
    }

    private Map<String, Object> map(Withdrawal w) {
        Map<String, Object> m = new LinkedHashMap<>();
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
