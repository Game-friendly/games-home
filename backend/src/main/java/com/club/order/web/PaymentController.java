package com.club.order.web;

import com.club.order.common.ApiResponse;
import com.club.order.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/orders/{id}/pay")
    public ApiResponse<Map<String, Object>> pay(@PathVariable Long id,
                                                @RequestAttribute("uid") Long uid) {
        paymentService.pay(id, uid);
        return ApiResponse.ok(Map.of("paid", true));
    }
}
