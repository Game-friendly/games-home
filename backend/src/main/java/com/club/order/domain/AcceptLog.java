package com.club.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 接单/抢单/退单动作审计。 */
@Entity
@Table(name = "accept_log")
@Getter
@Setter
@NoArgsConstructor
public class AcceptLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private Long workerId;

    /** 被抢时记录被顶掉的原持有者。 */
    private Long prevWorkerId;

    /** ACCEPT / STEAL / WITHDRAW / DISPATCH。 */
    private String action;

    /** 退单扣的信用分，0 表示无惩罚。 */
    private int penalty = 0;

    private LocalDateTime createdAt = LocalDateTime.now();
}
