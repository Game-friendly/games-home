package com.club.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 打手收益记录。订单确认完成时自动入账，管理员结算标记为已结算。 */
@Entity
@Table(name = "earnings")
@Getter
@Setter
@NoArgsConstructor
public class Earning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private Long workerId;

    /** 入账金额，单位分。P0 无抽成 = 订单预算全额。 */
    private int amount;

    /** PENDING 待结算 / SETTLED 已结算。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EarningStatus status = EarningStatus.PENDING;

    private LocalDateTime settledAt;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum EarningStatus {
        PENDING, SETTLED
    }
}
