package com.club.order.web.dto;

import com.club.order.domain.AssignmentStatus;
import com.club.order.domain.Order;
import com.club.order.domain.OrderAssignment;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 订单响应体。联系方式仅在 buyer/当前持有打手/admin 视角才返回。 */
public record OrderDTO(
        Long id,
        String orderNo,
        Long buyerId,
        String grade,
        String category,
        String title,
        String description,
        int budget,
        String status,
        String paymentStatus,
        LocalDateTime deadline,
        String contact,
        String address,
        String remark,
        List<String> images,
        int requiredWorkers,
        int filledWorkers,
        LocalDateTime createdAt,
        List<WorkerSlot> workers
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public record WorkerSlot(
            Long workerId,
            String workerGrade,
            String status,
            LocalDateTime reserveExpiresAt,
            LocalDateTime acceptedAt,
            LocalDateTime submittedAt
    ) {}

    public static OrderDTO of(Order o, List<OrderAssignment> assignments, Map<Long, String> grades, boolean withContact) {
        List<WorkerSlot> workers = assignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.RESERVED || a.getStatus() == AssignmentStatus.ACCEPTED)
                .map(a -> new WorkerSlot(
                        a.getWorkerId(),
                        grades.get(a.getWorkerId()),
                        a.getStatus().name(),
                        a.getReserveExpiresAt(),
                        a.getAcceptedAt(),
                        a.getSubmittedAt()))
                .toList();
        int filled = workers.size();
        return new OrderDTO(
                o.getId(),
                o.getOrderNo(),
                o.getBuyerId(),
                o.getGrade() == null ? null : o.getGrade().name(),
                o.getCategory(),
                o.getTitle(),
                o.getDescription(),
                o.getBudget(),
                o.getStatus().name(),
                o.getPaymentStatus() == null ? null : o.getPaymentStatus().name(),
                o.getDeadline(),
                withContact ? o.getContact() : null,
                o.getAddress(),
                o.getRemark(),
                parseImages(o.getImages()),
                o.getRequiredWorkers(),
                filled,
                o.getCreatedAt(),
                workers
        );
    }

    private static List<String> parseImages(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
