package com.club.order.service;

import com.club.order.common.BizException;
import com.club.order.domain.Message;
import com.club.order.domain.Order;
import com.club.order.domain.User;
import com.club.order.repo.MessageRepository;
import com.club.order.repo.OrderAssignmentRepository;
import com.club.order.repo.OrderRepository;
import com.club.order.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 订单留言：客户和该订单的接单人可在同一会话中收发消息。 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepo;
    private final OrderRepository orderRepo;
    private final OrderAssignmentRepository assignmentRepo;
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Long orderId, Long uid) {
        requireParticipant(orderId, uid);
        return messageRepo.findByOrderIdOrderByCreatedAtAscIdAsc(orderId).stream()
                .map(this::toMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> send(Long orderId, Long uid, String content) {
        requireParticipant(orderId, uid);
        if (content == null || content.isBlank()) {
            throw new BizException(400, "留言不能为空");
        }
        Message m = new Message();
        m.setOrderId(orderId);
        m.setSenderId(uid);
        m.setContent(content.trim());
        return toMap(messageRepo.save(m));
    }

    private void requireParticipant(Long orderId, Long uid) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new BizException(404, "订单不存在"));
        if (uid.equals(order.getBuyerId())) {
            return;
        }
        boolean isWorker = assignmentRepo.findByWorkerId(uid).stream()
                .anyMatch(a -> a.getOrderId().equals(orderId));
        if (!isWorker) {
            throw new BizException(403, "只有订单参与方可以留言");
        }
    }

    private Map<String, Object> toMap(Message m) {
        User sender = userRepo.findById(m.getSenderId()).orElse(null);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", m.getId());
        map.put("orderId", m.getOrderId());
        map.put("senderId", m.getSenderId());
        map.put("senderName", sender == null ? "用户" : sender.getNickname());
        map.put("senderRole", sender == null ? "" : sender.getRole().name());
        map.put("content", m.getContent());
        map.put("createdAt", m.getCreatedAt());
        return map;
    }
}
