package com.club.order.web;

import com.club.order.common.ApiResponse;
import com.club.order.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/api/orders/{id}/messages")
    public ApiResponse<List<Map<String, Object>>> list(@PathVariable Long id,
                                                       @RequestAttribute("uid") Long uid) {
        return ApiResponse.ok(messageService.list(id, uid));
    }

    @PostMapping("/api/orders/{id}/messages")
    public ApiResponse<Map<String, Object>> send(@PathVariable Long id,
                                                 @RequestBody Map<String, String> body,
                                                 @RequestAttribute("uid") Long uid) {
        return ApiResponse.ok(messageService.send(id, uid, body.get("content")));
    }
}
