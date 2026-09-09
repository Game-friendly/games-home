package com.club.order.web;

import com.club.order.common.ApiResponse;
import com.club.order.service.EarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/earnings")
@RequiredArgsConstructor
public class EarningController {

    private final EarningService earningService;

    /** 打手看自己的收益。 */
    @GetMapping("/mine")
    public ApiResponse<List<Map<String, Object>>> mine(@RequestAttribute("uid") Long uid) {
        return ApiResponse.ok(earningService.mine(uid));
    }
}
