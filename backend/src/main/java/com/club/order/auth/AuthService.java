package com.club.order.auth;

import com.club.order.domain.Role;
import com.club.order.domain.User;
import com.club.order.repo.UserRepository;
import com.club.order.web.dto.LoginRequest;
import com.club.order.web.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录。P0 用「code 即 openid」的 mock 方式，便于直接 curl 测试：
 *   demo_admin / demo_worker_a / demo_worker_b / demo_worker_c / demo_client_1
 * 未识别的 code 会新建一个客户账号。P1 再替换成微信 code 换 openid。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;
    private final WechatAuthClient wechatAuthClient;

    @Transactional
    public LoginResponse login(LoginRequest req) {
        String code = req.code() == null || req.code().isBlank() ? "demo_client_1" : req.code().trim();
        String openid = wechatAuthClient.openidForCode(code);
        User user = userRepo.findByOpenid(openid).orElseGet(() -> {
            User u = new User();
            u.setOpenid(openid);
            u.setNickname(code);
            u.setRole(Role.CLIENT);
            u.setGrade(com.club.order.domain.Grade.C);
            return userRepo.save(u);
        });
        String token = jwtUtil.generate(user.getId(), user.getRole().name());
        return new LoginResponse(token, user.getId(), user.getRole().name(), user.getGrade().name(), System.currentTimeMillis());
    }
}
