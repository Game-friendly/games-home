package com.club.order.web;

import com.club.order.common.ApiResponse;
import com.club.order.domain.Notification;
import com.club.order.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 站内通知（订阅消息的 P0 替代）。 */
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<Notification>> list(@RequestAttribute("uid") Long uid) {
        return ApiResponse.ok(notificationService.list(uid));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id, @RequestAttribute("uid") Long uid) {
        notificationService.markRead(id, uid);
        return ApiResponse.ok();
    }
}
