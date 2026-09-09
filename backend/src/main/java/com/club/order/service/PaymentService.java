package com.club.order.service;

/** 支付抽象。当前 mock 直接支付成功，后续接微信 JSAPI。 */
public interface PaymentService {
    /** 发起支付。mock 实现直接完成支付并发布订单。 */
    void pay(Long orderId, Long payerId);
}
