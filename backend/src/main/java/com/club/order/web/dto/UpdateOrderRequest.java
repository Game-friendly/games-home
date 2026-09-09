package com.club.order.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;
import java.util.List;

/** 管理员修改订单内容/价格/等级。字段均可选，传了就更新。 */
public record UpdateOrderRequest(
        String title,
        String category,
        String grade,
        String description,
        @Min(value = 0, message = "预算不能为负") Integer budget,
        @Min(value = 1, message = "人数至少为 1") @Max(value = 10, message = "人数最多为 10") Integer requiredWorkers,
        LocalDateTime deadline,
        String contact,
        String address,
        String remark,
        List<String> images
) {}
