package com.club.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 演示用支付实现：点击支付即成功。 */
@Service
@RequiredArgsConstructor
public class MockPaymentService implements PaymentService {

    private final OrderService orderService;

    @Override
    public void pay(Long orderId, Long payerId) {
        orderService.markPaid(orderId, payerId);
    }
}
