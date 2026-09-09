package com.club.order.service;

import com.club.order.common.BizException;
import com.club.order.config.ThemeProperties;
import com.club.order.domain.*;
import com.club.order.repo.AcceptLogRepository;
import com.club.order.repo.OrderAssignmentRepository;
import com.club.order.repo.OrderEventRepository;
import com.club.order.repo.OrderRepository;
import com.club.order.repo.UserRepository;
import com.club.order.web.dto.CreateOrderRequest;
import com.club.order.web.dto.OrderDTO;
import com.club.order.web.dto.UpdateOrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final UserRepository userRepo;
    private final AcceptLogRepository acceptLogRepo;
    private final OrderEventRepository eventRepo;
    private final NotificationService notificationService;
    private final EarningService earningService;
    private final OrderAssignmentRepository assignmentRepo;
    private final ObjectMapper objectMapper;
    private final ThemeProperties themeProperties;

    private static final List<AssignmentStatus> ACTIVE_STATUSES = List.of(
            AssignmentStatus.RESERVED, AssignmentStatus.ACCEPTED);

    @Value("${app.reserve-seconds:120}")
    private int reserveSeconds;

    @Value("${app.auto-confirm-hours:72}")
    private int autoConfirmHours;

    @Value("${app.min-accept-credit:60}")
    private int minAcceptCredit;

    // ---------- 客户侧 ----------

    @Transactional
    public OrderDTO create(CreateOrderRequest req, Long buyerId) {
        Order o = new Order();
        o.setOrderNo(genOrderNo());
        o.setBuyerId(buyerId);
        o.setAssignedWorkerId(req.assignedWorkerId());
        o.setGrade(parseGrade(req.grade(), Grade.C));
        validateCategory(req.category());
        o.setCategory(req.category());
        o.setTitle(req.title());
        o.setDescription(req.description());
        o.setBudget(req.budget());
        o.setRequiredWorkers(req.requiredWorkers() == null ? 1 : req.requiredWorkers());
        o.setDeadline(req.deadline());
        o.setContact(req.contact());
        o.setAddress(req.address());
        o.setRemark(req.remark());
        o.setImages(toJson(req.images()));
        o.setStatus(OrderStatus.PENDING_PAYMENT);
        o = orderRepo.save(o);
        event(o.getId(), OrderStatus.CREATED.name(), OrderStatus.PENDING_PAYMENT.name(), "client:" + buyerId);
        return dto(o, true);
    }

    /** 支付成功后把待支付订单发布到接单池。 */
    @Transactional
    public OrderDTO markPaid(Long orderId, Long payerId) {
        Order o = lock(orderId);
        if (!payerId.equals(o.getBuyerId())) {
            throw new BizException(403, "只有发单客户能支付");
        }
        if (o.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BizException(400, "订单不在待支付状态");
        }
        OrderStatus from = o.getStatus();
        o.setStatus(OrderStatus.PUBLISHED);
        o.setPaymentStatus(PaymentStatus.PAID);
        orderRepo.save(o);
        if (o.getAssignedWorkerId() != null) {
            assignDirect(o, o.getAssignedWorkerId());
            o = orderRepo.findById(orderId).orElse(o);
        }
        event(o.getId(), from.name(), o.getStatus().name(), "client:" + payerId);
        return dto(o, true);
    }

    @Transactional
    public OrderDTO cancelByBuyer(Long orderId, Long buyerId) {
        Order o = lock(orderId);
        if (!buyerId.equals(o.getBuyerId())) {
            throw new BizException(403, "只有发单客户能取消");
        }
        if ((o.getStatus() != OrderStatus.PUBLISHED && o.getStatus() != OrderStatus.PENDING_PAYMENT)
                || !activeAssignments(orderId).isEmpty()) {
            throw new BizException(400, "已有接单人，无法取消");
        }
        OrderStatus from = o.getStatus();
        o.setStatus(OrderStatus.CANCELLED);
        orderRepo.save(o);
        event(o.getId(), from.name(), o.getStatus().name(), "client:" + buyerId);
        return dto(o, true);
    }

    @Transactional
    public OrderDTO confirm(Long orderId, Long buyerId) {
        Order o = lock(orderId);
        if (!buyerId.equals(o.getBuyerId())) {
            throw new BizException(403, "只有发单客户能确认完成");
        }
        if (o.getStatus() != OrderStatus.DELIVERED) {
            throw new BizException(400, "订单不在待确认状态");
        }
        OrderStatus from = o.getStatus();
        o.setStatus(OrderStatus.COMPLETED);
        o.setCompletedAt(LocalDateTime.now());
        orderRepo.save(o);
        event(o.getId(), from.name(), o.getStatus().name(), "client:" + buyerId);
        List<OrderAssignment> active = activeAssignments(o.getId());
        earningService.credit(o, active);
        for (OrderAssignment a : active) {
            notificationService.send(a.getWorkerId(), "订单已完成",
                    "订单「" + o.getTitle() + "」已确认完成，你的收益已入账（待结算）");
        }
        return dto(o, true);
    }

    // ---------- 打手侧 ----------

    /** 接下一个空位，进入 2 分钟锁定保护期。多人的单要占满所有位置后才会进入 ACCEPTED。 */
    @Transactional
    public OrderDTO accept(Long orderId, Long workerId) {
        User worker = requireWorker(workerId);
        if (worker.getCreditScore() < minAcceptCredit) {
            throw new BizException(403, "信用分不足，暂时无法接单");
        }
        Order o = lock(orderId);
        if (o.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BizException(400, "订单尚未支付");
        }
        if (o.getStatus() != OrderStatus.PUBLISHED && o.getStatus() != OrderStatus.RESERVED) {
            throw new BizException(400, "该单当前不可接");
        }
        if (workerId.equals(o.getBuyerId())) {
            throw new BizException(403, "不能接自己发的单");
        }
        if (!canFirstAccept(worker, o)) {
            throw new BizException(403, "等级不足：A 级单仅 B 级及以上可接");
        }
        if (hasActiveAssignment(orderId, workerId)) {
            throw new BizException(400, "你已接下该单，不能重复接单");
        }
        if (activeAssignments(orderId).size() >= o.getRequiredWorkers()) {
            throw new BizException(400, "该单接单位置已满");
        }

        OrderStatus from = o.getStatus();
        OrderAssignment a = new OrderAssignment();
        a.setOrderId(orderId);
        a.setWorkerId(workerId);
        a.setStatus(AssignmentStatus.RESERVED);
        a.setReserveExpiresAt(LocalDateTime.now().plusSeconds(reserveSeconds));
        assignmentRepo.save(a);

        refresh(o);
        orderRepo.save(o);
        acceptLog(orderId, workerId, null, "ACCEPT", 0);
        event(orderId, from.name(), o.getStatus().name(), "worker:" + workerId);
        notificationService.send(o.getBuyerId(), "订单有接单人",
                "订单「" + o.getTitle() + "」已有 " + activeAssignments(orderId).size()
                        + "/" + o.getRequiredWorkers() + " 人接单");
        return dto(o, true);
    }

    /** 抢单：目标位置仍在 2 分钟保护期内时，严格更高等级可顶掉该位置，并重置保护期。 */
    @Transactional
    public OrderDTO steal(Long orderId, Long newWorkerId, Long targetWorkerId) {
        User worker = requireWorker(newWorkerId);
        if (worker.getCreditScore() < minAcceptCredit) {
            throw new BizException(403, "信用分不足，暂时无法抢单");
        }
        Order o = lock(orderId);
        if (newWorkerId.equals(o.getBuyerId())) {
            throw new BizException(403, "不能抢自己发的单");
        }
        if (hasActiveAssignment(orderId, newWorkerId)) {
            throw new BizException(400, "你已在该单的接单位置上");
        }

        List<OrderAssignment> active = activeAssignments(orderId);
        OrderAssignment target;
        if (targetWorkerId != null) {
            target = active.stream()
                    .filter(a -> targetWorkerId.equals(a.getWorkerId()))
                    .findFirst()
                    .orElseThrow(() -> new BizException(404, "目标接单人不存在"));
        } else {
            target = active.stream()
                    .filter(a -> a.getStatus() == AssignmentStatus.RESERVED)
                    .findFirst()
                    .orElseThrow(() -> new BizException(400, "当前没有可抢的位置"));
        }
        if (target.getStatus() != AssignmentStatus.RESERVED) {
            throw new BizException(400, "该位置已过保护期，无法抢单");
        }
        if (newWorkerId.equals(target.getWorkerId())) {
            throw new BizException(400, "该位置已由你持有");
        }

        User current = userRepo.findById(target.getWorkerId())
                .orElseThrow(() -> new BizException(404, "持有者不存在"));
        if (worker.getGrade().rank <= current.getGrade().rank) {
            throw new BizException(409, "等级不足：仅更高等级可抢（同级/低级不可抢）");
        }

        OrderStatus from = o.getStatus();
        target.setStatus(AssignmentStatus.CANCELLED);
        assignmentRepo.save(target);

        OrderAssignment next = new OrderAssignment();
        next.setOrderId(orderId);
        next.setWorkerId(newWorkerId);
        next.setStatus(AssignmentStatus.RESERVED);
        next.setReserveExpiresAt(LocalDateTime.now().plusSeconds(reserveSeconds));
        assignmentRepo.save(next);

        refresh(o);
        orderRepo.save(o);
        acceptLog(orderId, newWorkerId, target.getWorkerId(), "STEAL", 0);
        event(orderId, from.name(), o.getStatus().name(), "worker:" + newWorkerId);
        notificationService.send(target.getWorkerId(), "你的接单位置被抢",
                "订单「" + o.getTitle() + "」中你的位置已被 " + worker.getGrade() + " 级接单人顶替");
        notificationService.send(o.getBuyerId(), "订单接单人变更",
                "订单「" + o.getTitle() + "」中一个位置已由 " + worker.getGrade() + " 级接单人接手");
        return dto(o, true);
    }

    /** 退单：保护期内退单无惩罚；正式持有后退单扣信用分。 */
    @Transactional
    public OrderDTO withdraw(Long orderId, Long workerId) {
        Order o = lock(orderId);
        OrderAssignment a = assignmentRepo.findByOrderIdAndWorkerIdAndStatusIn(orderId, workerId, ACTIVE_STATUSES)
                .orElseThrow(() -> new BizException(403, "你不在该单的接单位置上"));
        if (a.getSubmittedAt() != null) {
            throw new BizException(400, "已交活，无法退单");
        }

        OrderStatus from = o.getStatus();
        int penalty = 0;
        if (a.getStatus() == AssignmentStatus.RESERVED) {
            // 保护期内退单，无惩罚
        } else if (a.getStatus() == AssignmentStatus.ACCEPTED) {
            penalty = 5;
            User w = userRepo.findById(workerId).orElseThrow();
            w.setCreditScore(w.getCreditScore() - penalty);
            userRepo.save(w);
        } else {
            throw new BizException(400, "当前状态不可退单");
        }
        a.setStatus(AssignmentStatus.CANCELLED);
        assignmentRepo.save(a);
        refresh(o);
        orderRepo.save(o);
        acceptLog(orderId, workerId, null, "WITHDRAW", penalty);
        event(orderId, from.name(), o.getStatus().name(), "worker:" + workerId);
        notificationService.send(o.getBuyerId(), "接单人退单",
                "订单「" + o.getTitle() + "」有空位重新开放"
                        + (penalty > 0 ? "（接单人信用分 -" + penalty + "）" : ""));
        return dto(o, true);
    }

    /** 某个接单人提交完成。所有位置都交活后，订单进入 DELIVERED。 */
    @Transactional
    public OrderDTO complete(Long orderId, Long workerId) {
        Order o = lock(orderId);
        OrderAssignment a = assignmentRepo.findByOrderIdAndWorkerIdAndStatusIn(orderId, workerId, ACTIVE_STATUSES)
                .orElseThrow(() -> new BizException(403, "你不在该单的接单位置上"));
        if (a.getStatus() != AssignmentStatus.ACCEPTED || a.getSubmittedAt() != null) {
            throw new BizException(400, "当前状态不可提交完成");
        }

        OrderStatus from = o.getStatus();
        a.setSubmittedAt(LocalDateTime.now());
        assignmentRepo.save(a);
        refresh(o);
        orderRepo.save(o);
        event(orderId, from.name(), o.getStatus().name(), "worker:" + workerId);
        if (o.getStatus() == OrderStatus.DELIVERED) {
            notificationService.send(o.getBuyerId(), "订单已交活",
                    "订单「" + o.getTitle() + "」所有接单人已提交完成，请确认");
        } else {
            notificationService.send(o.getBuyerId(), "有接单人已交活",
                    "订单「" + o.getTitle() + "」已有接单人提交完成，仍在等待其他人");
        }
        return dto(o, true);
    }

    // ---------- 查询 ----------

    public List<OrderDTO> pool() {
        return orderRepo.findByStatusIn(
                        List.of(OrderStatus.PUBLISHED, OrderStatus.RESERVED),
                        Sort.by(Sort.Direction.DESC, "id"))
                .stream().map(o -> dto(o, false)).toList();
    }

    public List<OrderDTO> myOrders(Long buyerId) {
        return orderRepo.findByBuyerId(buyerId, Sort.by(Sort.Direction.DESC, "id"))
                .stream().map(o -> dto(o, true)).toList();
    }

    public List<OrderDTO> accepted(Long workerId) {
        return assignmentRepo.findByWorkerIdAndStatusIn(workerId, ACTIVE_STATUSES).stream()
                .map(a -> orderRepo.findById(a.getOrderId()).orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Order::getId).reversed())
                .map(o -> dto(o, true))
                .toList();
    }

    public OrderDTO detail(Long orderId, Long viewerId, Role role) {
        Order o = orderRepo.findById(orderId).orElseThrow(() -> new BizException(404, "订单不存在"));
        boolean withContact = role == Role.ADMIN
                || viewerId.equals(o.getBuyerId())
                || hasActiveAssignment(orderId, viewerId);
        return dto(o, withContact);
    }

    // ---------- 定时任务 ----------

    @Transactional
    public int finalizeExpired() {
        List<OrderAssignment> expired = assignmentRepo.findByStatusAndReserveExpiresAtBefore(
                AssignmentStatus.RESERVED, LocalDateTime.now());
        Set<Long> orderIds = new HashSet<>();
        int n = 0;
        for (OrderAssignment e : expired) {
            OrderAssignment a = assignmentRepo.findByIdForUpdate(e.getId()).orElse(null);
            if (a != null && a.getStatus() == AssignmentStatus.RESERVED) {
                a.setStatus(AssignmentStatus.ACCEPTED);
                a.setAcceptedAt(LocalDateTime.now());
                assignmentRepo.save(a);
                orderIds.add(a.getOrderId());
                n++;
            }
        }
        for (Long orderId : orderIds) {
            Order o = orderRepo.findByIdForUpdate(orderId).orElse(null);
            if (o != null) {
                OrderStatus from = o.getStatus();
                refresh(o);
                orderRepo.save(o);
                if (from != o.getStatus()) {
                    event(orderId, from.name(), o.getStatus().name(), "system");
                }
            }
        }
        return n;
    }

    /** 截止时间到且完全没有接单人的单自动取消；有部分接单人的单保留待补位。 */
    @Transactional
    public int cancelOverduePublished() {
        List<Order> overdue = orderRepo.findByStatusAndDeadlineBefore(OrderStatus.PUBLISHED, LocalDateTime.now());
        int n = 0;
        for (Order e : overdue) {
            Order o = orderRepo.findByIdForUpdate(e.getId()).orElse(null);
            if (o != null && o.getStatus() == OrderStatus.PUBLISHED && activeAssignments(o.getId()).isEmpty()) {
                OrderStatus from = o.getStatus();
                o.setStatus(OrderStatus.CANCELLED);
                orderRepo.save(o);
                event(o.getId(), from.name(), o.getStatus().name(), "system");
                notificationService.send(o.getBuyerId(), "订单超时取消",
                        "订单「" + o.getTitle() + "」超过截止时间无人接单，已自动取消");
                n++;
            }
        }
        return n;
    }

    /** 所有接单人都交活后，客户超时未确认自动完成。 */
    @Transactional
    public int autoConfirmDelivered() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(autoConfirmHours);
        List<Order> overdue = orderRepo.findByStatusAndDeliveredAtBefore(OrderStatus.DELIVERED, threshold);
        int n = 0;
        for (Order e : overdue) {
            Order o = orderRepo.findByIdForUpdate(e.getId()).orElse(null);
            if (o != null && o.getStatus() == OrderStatus.DELIVERED) {
                OrderStatus from = o.getStatus();
                o.setStatus(OrderStatus.COMPLETED);
                o.setCompletedAt(LocalDateTime.now());
                orderRepo.save(o);
                event(o.getId(), from.name(), o.getStatus().name(), "system");
                List<OrderAssignment> active = activeAssignments(o.getId());
                earningService.credit(o, active);
                for (OrderAssignment a : active) {
                    notificationService.send(a.getWorkerId(), "订单超时自动确认完成",
                            "订单「" + o.getTitle() + "」客户超时未确认，已自动完成，收益已入账（待结算）");
                }
                n++;
            }
        }
        return n;
    }

    // ---------- 管理员 ----------

    @Transactional
    public OrderDTO updateByAdmin(Long orderId, UpdateOrderRequest req) {
        Order o = orderRepo.findById(orderId).orElseThrow(() -> new BizException(404, "订单不存在"));
        if (req.requiredWorkers() != null) {
            int filled = activeAssignments(orderId).size();
            if (req.requiredWorkers() < filled) {
                throw new BizException(400, "人数不能小于当前已接人数");
            }
            o.setRequiredWorkers(req.requiredWorkers());
        }
        if (req.title() != null) o.setTitle(req.title());
        if (req.category() != null) {
            validateCategory(req.category());
            o.setCategory(req.category());
        }
        if (req.grade() != null) o.setGrade(parseGrade(req.grade(), o.getGrade()));
        if (req.description() != null) o.setDescription(req.description());
        if (req.budget() != null) o.setBudget(req.budget());
        if (req.deadline() != null) o.setDeadline(req.deadline());
        if (req.contact() != null) o.setContact(req.contact());
        if (req.address() != null) o.setAddress(req.address());
        if (req.remark() != null) o.setRemark(req.remark());
        if (req.images() != null) o.setImages(toJson(req.images()));
        if (o.getStatus() != OrderStatus.COMPLETED
                && o.getStatus() != OrderStatus.CANCELLED
                && o.getStatus() != OrderStatus.DISPUTED) {
            refresh(o);
        }
        orderRepo.save(o);
        event(o.getId(), o.getStatus().name(), o.getStatus().name(), "admin");
        notificationService.send(o.getBuyerId(), "管理员修改了订单",
                "订单「" + o.getTitle() + "」的内容/人数/价格被管理员调整，请查看");
        return dto(o, true);
    }

    /** 管理员派单：绕过抢单，直接填入一个接单位置。 */
    @Transactional
    public OrderDTO dispatch(Long orderId, Long workerId) {
        Order o = lock(orderId);
        User worker = requireWorker(workerId);
        if (o.getStatus() != OrderStatus.PUBLISHED) {
            throw new BizException(400, "只有待补位的单可派单");
        }
        if (workerId.equals(o.getBuyerId())) {
            throw new BizException(403, "不能派给发单客户");
        }
        if (hasActiveAssignment(orderId, workerId)) {
            throw new BizException(400, "该接单人已在这单里");
        }
        if (activeAssignments(orderId).size() >= o.getRequiredWorkers()) {
            throw new BizException(400, "接单位置已满");
        }

        OrderStatus from = o.getStatus();
        OrderAssignment a = new OrderAssignment();
        a.setOrderId(orderId);
        a.setWorkerId(workerId);
        a.setStatus(AssignmentStatus.ACCEPTED);
        a.setAcceptedAt(LocalDateTime.now());
        assignmentRepo.save(a);
        refresh(o);
        orderRepo.save(o);
        acceptLog(orderId, workerId, null, "DISPATCH", 0);
        event(orderId, from.name(), o.getStatus().name(), "admin");
        notificationService.send(workerId, "管理员派单",
                "管理员为你指派了订单「" + o.getTitle() + "」，请及时处理");
        return dto(o, true);
    }

    /** 客户指定大神下单：支付成功后直接填入该接单人（单人单进入进行中，多人单填一个位置后回大厅）。 */
    private void assignDirect(Order o, Long workerId) {
        User worker = requireWorker(workerId);
        if (worker.getCreditScore() < minAcceptCredit) {
            throw new BizException(403, "该接单人信用分不足，无法指定接单");
        }
        if (workerId.equals(o.getBuyerId())) {
            throw new BizException(403, "不能指定发单客户接单");
        }
        if (!canFirstAccept(worker, o)) {
            throw new BizException(403, "等级不足：A 级单仅 B 级及以上可接");
        }
        if (hasActiveAssignment(o.getId(), workerId)) {
            throw new BizException(400, "该接单人已在这单里");
        }
        if (activeAssignments(o.getId()).size() >= o.getRequiredWorkers()) {
            throw new BizException(400, "接单位置已满");
        }
        OrderStatus from = o.getStatus();
        OrderAssignment a = new OrderAssignment();
        a.setOrderId(o.getId());
        a.setWorkerId(workerId);
        a.setStatus(AssignmentStatus.ACCEPTED);
        a.setAcceptedAt(LocalDateTime.now());
        assignmentRepo.save(a);
        refresh(o);
        orderRepo.save(o);
        acceptLog(o.getId(), workerId, null, "ASSIGN", 0);
        event(o.getId(), from.name(), o.getStatus().name(), "client:" + o.getBuyerId());
        notificationService.send(workerId, "客户指定接单",
                "客户指定你处理订单「" + o.getTitle() + "」，请及时查看");
    }

    // ---------- helpers ----------

    private Order lock(Long orderId) {
        return orderRepo.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BizException(404, "订单不存在"));
    }

    private User requireWorker(Long workerId) {
        User u = userRepo.findById(workerId).orElseThrow(() -> new BizException(404, "用户不存在"));
        if (u.getRole() != Role.WORKER) {
            throw new BizException(403, "仅接单人可操作");
        }
        return u;
    }

    private Grade parseGrade(String s, Grade def) {
        if (s == null || s.isBlank()) return def;
        try {
            return Grade.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(400, "等级只能是 A/B/C");
        }
    }

    private void validateCategory(String category) {
        if (category != null && !category.isBlank() && !themeProperties.isValidCategory(category)) {
            throw new BizException(400, "分类不在当前主题分类列表内");
        }
    }

    private boolean canFirstAccept(User worker, Order order) {
        return worker.getGrade().rank >= order.getGrade().rank - 1;
    }

    private boolean hasActiveAssignment(Long orderId, Long workerId) {
        return assignmentRepo.findByOrderIdAndWorkerIdAndStatusIn(orderId, workerId, ACTIVE_STATUSES).isPresent();
    }

    private List<OrderAssignment> activeAssignments(Long orderId) {
        return assignmentRepo.findByOrderIdAndStatusIn(orderId, ACTIVE_STATUSES);
    }

    private List<OrderAssignment> assignments(Long orderId) {
        return assignmentRepo.findByOrderIdOrderByIdAsc(orderId);
    }

    /** 根据当前有效位置重新推导订单整体状态。 */
    private void refresh(Order o) {
        List<OrderAssignment> active = activeAssignments(o.getId());
        int filled = active.size();
        if (filled < o.getRequiredWorkers()) {
            o.setStatus(OrderStatus.PUBLISHED);
            return;
        }
        boolean hasReserved = active.stream().anyMatch(a -> a.getStatus() == AssignmentStatus.RESERVED);
        if (hasReserved) {
            o.setStatus(OrderStatus.RESERVED);
            return;
        }
        boolean allSubmitted = active.stream().allMatch(a -> a.getSubmittedAt() != null);
        if (allSubmitted) {
            o.setStatus(OrderStatus.DELIVERED);
            if (o.getDeliveredAt() == null) {
                o.setDeliveredAt(LocalDateTime.now());
            }
        } else {
            o.setStatus(OrderStatus.ACCEPTED);
        }
    }

    private OrderDTO dto(Order o, boolean withContact) {
        List<OrderAssignment> all = assignments(o.getId());
        Map<Long, String> grades = new LinkedHashMap<>();
        for (OrderAssignment a : all) {
            if (!grades.containsKey(a.getWorkerId())) {
                userRepo.findById(a.getWorkerId()).ifPresent(u -> grades.put(u.getId(), u.getGrade().name()));
            }
        }
        return OrderDTO.of(o, all, grades, withContact);
    }

    private void acceptLog(Long orderId, Long workerId, Long prevWorkerId, String action, int penalty) {
        AcceptLog log = new AcceptLog();
        log.setOrderId(orderId);
        log.setWorkerId(workerId);
        log.setPrevWorkerId(prevWorkerId);
        log.setAction(action);
        log.setPenalty(penalty);
        acceptLogRepo.save(log);
    }

    private void event(Long orderId, String from, String to, String operator) {
        eventRepo.save(new OrderEvent(orderId, from, to, operator));
    }

    private String genOrderNo() {
        return "O" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private String toJson(List<String> values) {
        if (values == null || values.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            throw new BizException(500, "图片信息处理失败");
        }
    }
}
