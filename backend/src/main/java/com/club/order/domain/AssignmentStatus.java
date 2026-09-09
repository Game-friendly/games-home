package com.club.order.domain;

/** 单个接单位置的状态。订单整体状态由 OrderService 汇总这些位置得出。 */
public enum AssignmentStatus {
    RESERVED,   // 刚接单，处于 2 分钟保护期
    ACCEPTED,   // 保护期结束，正式持有该位置
    CANCELLED   // 退单或被抢单顶掉
}
