package com.club.order.service;

import com.club.order.domain.Notification;
import com.club.order.repo.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 站内通知。P0 为订阅消息的桩实现：事件发生时写通知表，前端轮询拉取。
 * 以后拿到小程序资质，在 send() 里补微信订阅消息推送即可，调用方不变。
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepo;

    /** 发通知。异步推送(微信订阅消息)在 P1 接入。 */
    @Transactional
    public void send(Long userId, String title, String body) {
        if (userId == null) return;
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setBody(body);
        notificationRepo.save(n);
    }

    public List<Notification> list(Long userId) {
        return notificationRepo.findByUserId(userId, Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional
    public void markRead(Long id, Long userId) {
        notificationRepo.findById(id).ifPresent(n -> {
            if (userId.equals(n.getUserId())) {
                n.setRead(true);
                notificationRepo.save(n);
            }
        });
    }
}
