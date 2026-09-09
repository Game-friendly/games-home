package com.club.order.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReviewRequest(
        @NotNull(message = "请选择被评价的接单人") Long toId,
        @Min(value = 1, message = "评分须为 1-5") @Max(value = 5, message = "评分须为 1-5") int rating,
        String comment
) {}
