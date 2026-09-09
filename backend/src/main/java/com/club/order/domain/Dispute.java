package com.club.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 订单争议。客户或接单人发起，管理员裁决。 */
@Entity
@Table(name = "disputes")
@Getter
@Setter
@NoArgsConstructor
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    /** 发起人 userId。 */
    private Long createdBy;

    @Column(length = 2000)
    private String reason;

    /** 发起争议前的订单状态，裁决为 RELEASE 时恢复。 */
    @Enumerated(EnumType.STRING)
    private OrderStatus previousStatus;

    @Enumerated(EnumType.STRING)
    private DisputeStatus status = DisputeStatus.OPEN;

    @Enumerated(EnumType.STRING)
    private DisputeOutcome outcome;

    @Column(length = 2000)
    private String resolution;

    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public enum DisputeStatus {
        OPEN, RESOLVED
    }

    public enum DisputeOutcome {
        RELEASE, REFUND, CANCEL
    }
}
