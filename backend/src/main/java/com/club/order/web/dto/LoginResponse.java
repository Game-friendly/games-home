package com.club.order.web.dto;

public record LoginResponse(String token, Long userId, String role, String grade, Long serverTime) {}
