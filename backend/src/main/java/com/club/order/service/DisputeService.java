package com.club.order.service;

import com.club.order.common.BizException;
import com.club.order.domain.AssignmentStatus;
import com.club.order.domain.Dispute;
import com.club.order.domain.Order;
import com.club.order.domain.OrderAssignment;
import com.club.order.domain.OrderStatus;
import com.club.order.repo.DisputeRepository;
import com.club.order.repo.OrderAssignmentRepository;
import com.club.order.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DisputeService {

    private static final Set<OrderStatus> DISPUTABLE_STATUSES = Set.of(
            OrderStatus.ACCEPTED, OrderStatus.DELIVERED, OrderStatus.COMPLETED);
    private static final List<AssignmentStatus> ACTIVE = List.of(
            AssignmentStatus.RESERVED, AssignmentStatus.ACCEPTED);

    private final OrderRepository orderRepo;
    private final OrderAssignmentRepository assignmentRepo;
    private final DisputeRepository disputeRepo;
    private final NotificationService notificationService;

    @Transactional
    public Dispute open(Long orderId, Long userId, String reason) {
        Order o = orderRepo.findById(orderId).orElseThrow(() -> new BizException(404, "订单不存在"));
        if (!DISPUTABLE_STATUSES.contains(o.getStatus())) {
            throw new BizException(400, "当前状态不可发起争议");
        }
        boolean isBuyer = userId.equals(o.getBuyerId());
        boolean isWorker = assignmentRepo.findByOrderIdAndWorkerIdAndStatusIn(orderId, userId, ACTIVE).isPresent();
        if (!isBuyer && !isWorker) {
            throw new BizException(403, "只有订单客户或接单人能发起争议");
        }
        if (disputeRepo.findByOrderIdAndStatus(orderId, Dispute.DisputeStatus.OPEN).isPresent()) {
            throw new BizException(400, "该订单已有待处理争议");
        }

        Dispute d = new Dispute();
        d.setOrderId(orderId);
        d.setCreatedBy(userId);
        d.setReason(reason);
        d.setPreviousStatus(o.getStatus());
        o.setStatus(OrderStatus.DISPUTED);
        orderRepo.save(o);
        disputeRepo.save(d);

        notificationService.send(o.getBuyerId(), "订单进入争议",
                "订单「" + o.getTitle() + "」已进入争议处理，请等待平台裁决");
        for (OrderAssignment a : assignmentRepo.findByOrderIdAndStatusIn(orderId, ACTIVE)) {
            if (!userId.equals(a.getWorkerId())) {
                notificationService.send(a.getWorkerId(), "订单进入争议",
                        "订单「" + o.getTitle() + "」已进入争议处理，请等待平台裁决");
            }
        }
        return d;
    }

    public List<Dispute> listAll() {
        return disputeRepo.findAllByOrderByIdDesc();
    }

    @Transactional
    public Dispute resolve(Long disputeId, Dispute.DisputeOutcome outcome, String resolution) {
        Dispute d = disputeRepo.findById(disputeId).orElseThrow(() -> new BizException(404, "争议不存在"));
        if (d.getStatus() != Dispute.DisputeStatus.OPEN) {
            throw new BizException(400, "争议已处理");
        }
        Order o = orderRepo.findById(d.getOrderId()).orElseThrow(() -> new BizException(404, "订单不存在"));
        d.setOutcome(outcome);
        d.setResolution(resolution);
        d.setStatus(Dispute.DisputeStatus.RESOLVED);
        d.setResolvedAt(LocalDateTime.now());
        if (outcome == Dispute.DisputeOutcome.RELEASE) {
            o.setStatus(d.getPreviousStatus() == null ? OrderStatus.DELIVERED : d.getPreviousStatus());
        } else if (outcome == Dispute.DisputeOutcome.REFUND) {
            o.setStatus(OrderStatus.REFUNDED);
            o.setPaymentStatus(com.club.order.domain.PaymentStatus.REFUNDED);
        } else {
            o.setStatus(OrderStatus.CANCELLED);
        }
        disputeRepo.save(d);
        orderRepo.save(o);

        notificationService.send(o.getBuyerId(), "争议裁决完成",
                "订单「" + o.getTitle() + "」的争议已处理：" + (resolution == null ? "" : resolution));
        for (OrderAssignment a : assignmentRepo.findByOrderIdAndStatusIn(d.getOrderId(), ACTIVE)) {
            notificationService.send(a.getWorkerId(), "争议裁决完成",
                    "订单「" + o.getTitle() + "」的争议已处理：" + (resolution == null ? "" : resolution));
        }
        return d;
    }
}
