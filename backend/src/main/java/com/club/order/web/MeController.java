package com.club.order.web;

import com.club.order.common.ApiResponse;
import com.club.order.domain.User;
import com.club.order.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 当前登录用户信息（含信用分，前端展示用）。 */
@RestController
@RequiredArgsConstructor
public class MeController {

    private final UserRepository userRepo;

    @GetMapping("/api/me")
    public ApiResponse<Map<String, Object>> me(@RequestAttribute("uid") Long uid) {
        User u = userRepo.findById(uid).orElse(null);
        Map<String, Object> m = new LinkedHashMap<>();
        if (u == null) {
            return ApiResponse.ok(m);
        }
        m.put("id", u.getId());
        m.put("userId", u.getId());
        m.put("nickname", u.getNickname());
        m.put("role", u.getRole().name());
        m.put("grade", u.getGrade().name());
        m.put("creditScore", u.getCreditScore());
        m.put("serverTime", System.currentTimeMillis());
        return ApiResponse.ok(m);
    }
}
