package com.club.order.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "标题不能为空") String title,
        String category,
        String grade,
        String description,
        @NotNull(message = "预算不能为空") @Min(value = 0, message = "预算不能为负") Integer budget,
        @Min(value = 1, message = "人数至少为 1") @Max(value = 10, message = "人数最多为 10") Integer requiredWorkers,
        LocalDateTime deadline,
        String contact,
        String address,
        String remark,
        List<String> images,
        Long assignedWorkerId
) {}
