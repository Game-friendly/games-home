package com.club.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 接单人提现申请。 */
@Entity
@Table(name = "withdrawals")
@Getter
@Setter
@NoArgsConstructor
public class Withdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long workerId;

    /** 申请金额，单位分。 */
    private int amount;

    @Enumerated(EnumType.STRING)
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    private LocalDateTime createdAt;

    private LocalDateTime handledAt;

    @Column(length = 1000)
    private String remark;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public enum WithdrawalStatus {
        PENDING, PAID, REJECTED
    }
}
