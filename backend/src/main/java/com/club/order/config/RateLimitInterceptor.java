package com.club.order.config;

import com.club.order.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简易接口限流（防脚本刷抢单）。
 * 对写操作接口按用户做滑动窗口限流：10 秒内最多 30 次。
 * P1 多实例部署时换 Redis 计数。
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final long WINDOW_MS = 10_000L;
    private static final int MAX_REQ = 30;

    private final Map<Long, Deque<Long>> counters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!"POST".equalsIgnoreCase(request.getMethod()) && !"PATCH".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        Object uid = request.getAttribute("uid");
        if (uid == null) {
            return true; // 未登录由 AuthFilter 处理
        }
        long now = System.currentTimeMillis();
        Deque<Long> deque = counters.computeIfAbsent((Long) uid, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && now - deque.peekFirst() > WINDOW_MS) {
                deque.pollFirst();
            }
            if (deque.size() >= MAX_REQ) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(
                        ApiResponse.fail(429, "操作太频繁，请稍后再试")));
                return false;
            }
            deque.addLast(now);
        }
        return true;
    }
}
