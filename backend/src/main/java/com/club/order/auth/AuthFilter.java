package com.club.order.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.club.order.common.ApiResponse;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** 简易 JWT 鉴权过滤器：解析 token，把 uid / role 写入请求属性，供控制器 @RequestAttribute 读取。 */
@Component
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login", "/api/theme", "/ping", "/h2-console", "/error", "/uploads");

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI();
        for (String p : PUBLIC_PATHS) {
            if (path.startsWith(p)) {
                chain.doFilter(req, resp);
                return;
            }
        }
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parse(header.substring(7));
                req.setAttribute("uid", Long.valueOf(claims.getSubject()));
                req.setAttribute("role", claims.get("role", String.class));
                chain.doFilter(req, resp);
                return;
            } catch (Exception ignored) {
                // 落到 401
            }
        }
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(401, "未登录或 token 失效")));
    }
}
