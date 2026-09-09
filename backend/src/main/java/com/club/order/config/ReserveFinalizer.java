package com.club.order.config;

import com.club.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定时固化：扫过保护期的锁定单（RESERVED），自动转 ACCEPTED。 */
@Component
@RequiredArgsConstructor
public class ReserveFinalizer {

    private final OrderService orderService;

    @Scheduled(fixedDelay = 3000)
    public void finalizeReserved() {
        try {
            orderService.finalizeExpired();
        } catch (Exception ignored) {
            // 定时任务吞异常，避免中断调度
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void runTimeouts() {
        try {
            orderService.cancelOverduePublished();
            orderService.autoConfirmDelivered();
        } catch (Exception ignored) {
        }
    }
}
