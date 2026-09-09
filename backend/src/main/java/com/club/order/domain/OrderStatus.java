package com.club.order.domain;

/** 订单状态机。转移规则集中在 OrderService 里校验。 */
public enum OrderStatus {
    CREATED,    // 已创建（下单、未发布）
    PENDING_PAYMENT, // 待支付
    PUBLISHED,  // 待接单（在单池，打手可见）
    RESERVED,   // 锁定中（已被接，2分钟保护期，高等级可抢）
    ACCEPTED,   // 进行中（锁定到期，正式归某打手）
    DELIVERED,  // 待确认（打手已交活）
    COMPLETED,  // 已完成（已结算）
    CANCELLED,  // 已取消
    REFUNDED,   // 已退款
    DISPUTED    // 争议中
}
