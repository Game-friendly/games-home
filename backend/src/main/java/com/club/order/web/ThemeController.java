package com.club.order.web;

import com.club.order.common.ApiResponse;
import com.club.order.config.ThemeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ThemeController {

    private final ThemeProperties theme;

    @GetMapping("/api/theme")
    public ApiResponse<ThemeProperties> theme() {
        return ApiResponse.ok(theme);
    }
}
