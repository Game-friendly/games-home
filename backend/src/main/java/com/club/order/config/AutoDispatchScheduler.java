package com.club.order.config;

import com.club.order.service.AutoDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutoDispatchScheduler {

    private final AutoDispatchService autoDispatchService;

    @Scheduled(fixedDelay = 60_000)
    public void dispatch() {
        try {
            autoDispatchService.dispatchAll();
        } catch (Exception ignored) {
        }
    }
}
