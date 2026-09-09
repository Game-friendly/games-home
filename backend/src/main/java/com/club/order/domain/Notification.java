package com.club.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 站内通知（订阅消息的桩实现）。
 * P0 无小程序资质拿不到订阅消息模板，用 DB 通知替代；以后接真实订阅消息
 * 只需在 NotificationService 的 send() 里加微信推送，接口不变。
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String title;

    @Column(length = 500)
    private String body;

    private boolean read = false;

    private LocalDateTime createdAt = LocalDateTime.now();
}
