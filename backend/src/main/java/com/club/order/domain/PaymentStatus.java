package com.club.order.domain;

/** 订单支付状态。PENDING 未支付，PAID 已支付，REFUNDED 已退款。 */
public enum PaymentStatus {
    PENDING, PAID, REFUNDED
}
