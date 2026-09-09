package com.club.order.web;

import com.club.order.common.ApiResponse;
import com.club.order.domain.Role;
import com.club.order.service.OrderService;
import com.club.order.web.dto.CompleteRequest;
import com.club.order.web.dto.CreateOrderRequest;
import com.club.order.web.dto.OrderDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ---- 客户 ----
    @PostMapping
    public ApiResponse<OrderDTO> create(@Valid @RequestBody CreateOrderRequest req,
                                        @RequestAttribute("uid") Long uid) {
        return ApiResponse.ok(orderService.create(req, uid));
    }

    @GetMapping("/my")
    public ApiResponse<List<OrderDTO>> my(@RequestAttribute("uid") Long uid) {
        return ApiResponse.ok(orderService.myOrders(uid));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<OrderDTO> confirm(@PathVariable Long id, @RequestAttribute("uid") Long uid) {
        return ApiResponse.ok(orderService.confirm(id, uid));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderDTO> cancel(@PathVariable Long id, @RequestAttribute("uid") Long uid) {
        return ApiResponse.ok(orderService.cancelByBuyer(id, uid));
    }

    // ---- 打手 ----
    @GetMapping("/pool")
    public ApiResponse<List<OrderDTO>> pool() {
        return ApiResponse.ok(orderService.pool());
    }

    @GetMapping("/accepted")
    public ApiResponse<List<OrderDTO>> accepted(@RequestAttribute("uid") Long uid) {
        return ApiResponse.ok(orderService.accepted(uid));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<OrderDTO> accept(@PathVariable Long id, @RequestAttribute("uid") Long uid) {
        return ApiResponse.ok(orderService.accept(id, uid));
    }

    @PostMapping("/{id}/steal")
    public ApiResponse<OrderDTO> steal(@PathVariable Long id,
                                       @RequestBody(required = false) Map<String, Long> body,
                                       @RequestAttribute("uid") Long uid) {
        Long targetWorkerId = body == null ? null : body.get("targetWorkerId");
        return ApiResponse.ok(orderService.steal(id, uid, targetWorkerId));
    }

    @PostMapping("/{id}/withdraw")
    public ApiResponse<OrderDTO> withdraw(@PathVariable Long id, @RequestAttribute("uid") Long uid) {
        return ApiResponse.ok(orderService.withdraw(id, uid));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<OrderDTO> complete(@PathVariable Long id,
                                          @RequestBody(required = false) CompleteRequest req,
                                          @RequestAttribute("uid") Long uid) {
        return ApiResponse.ok(orderService.complete(id, uid));
    }

    // ---- 通用 ----
    @GetMapping("/{id}")
    public ApiResponse<OrderDTO> detail(@PathVariable Long id,
                                        @RequestAttribute("uid") Long uid,
                                        @RequestAttribute("role") String role) {
        return ApiResponse.ok(orderService.detail(id, uid, Role.valueOf(role)));
    }
}
