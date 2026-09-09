package com.club.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNo;

    /** 发单客户（简化：存 id 而非外键，P0 够用）。 */
    @Column(nullable = false)
    private Long buyerId;

    /** 客户指定接单人（选人下单）。为空则走大厅抢单。 */
    private Long assignedWorkerId;

    /** 单的难度等级 A/B/C。A 单只有 B 级及以上打手能首接。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Grade grade = Grade.C;

    /** 需要几人一起接单。1 表示单人单，2 表示双人单。 */
    @Column(nullable = false)
    private int requiredWorkers = 1;

    private String category;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    /** 图片 URL 列表，JSON 字符串。P0 无图，留空。 */
    @Column(length = 2000)
    private String images;

    /** 预算，单位分。 */
    private int budget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private LocalDateTime deadline;

    private String contact;

    private String address;

    private String remark;

    /** 所有接单位置都交活、订单进入 DELIVERED 的时间，用于超时自动确认。 */
    private LocalDateTime deliveredAt;

    private LocalDateTime completedAt;

    @Version
    private int version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
