package com.club.order.auth;

import com.club.order.common.ApiResponse;
import com.club.order.web.dto.LoginRequest;
import com.club.order.web.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }
}
