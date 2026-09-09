package com.club.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 状态流转审计。 */
@Entity
@Table(name = "order_events")
@Getter
@Setter
@NoArgsConstructor
public class OrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private String fromStatus;

    private String toStatus;

    /** 触发方，如 worker:2 / client:1 / admin:3 / system。 */
    private String operator;

    private LocalDateTime createdAt = LocalDateTime.now();

    public OrderEvent(Long orderId, String fromStatus, String toStatus, String operator) {
        this.orderId = orderId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.operator = operator;
    }
}
