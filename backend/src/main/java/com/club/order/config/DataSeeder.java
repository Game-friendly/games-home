package com.club.order.config;

import com.club.order.domain.Grade;
import com.club.order.domain.AssignmentStatus;
import com.club.order.domain.Dispute;
import com.club.order.domain.Earning;
import com.club.order.domain.Message;
import com.club.order.domain.Notification;
import com.club.order.domain.Order;
import com.club.order.domain.OrderAssignment;
import com.club.order.domain.OrderStatus;
import com.club.order.domain.PaymentStatus;
import com.club.order.domain.Review;
import com.club.order.domain.Role;
import com.club.order.domain.User;
import com.club.order.domain.Withdrawal;
import com.club.order.repo.DisputeRepository;
import com.club.order.repo.EarningRepository;
import com.club.order.repo.MessageRepository;
import com.club.order.repo.NotificationRepository;
import com.club.order.repo.OrderAssignmentRepository;
import com.club.order.repo.OrderRepository;
import com.club.order.repo.ReviewRepository;
import com.club.order.repo.UserRepository;
import com.club.order.repo.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 启动时造演示账号，便于直接登录测试。 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepo;
    private final OrderRepository orderRepo;
    private final OrderAssignmentRepository assignmentRepo;
    private final EarningRepository earningRepo;
    private final ReviewRepository reviewRepo;
    private final NotificationRepository notificationRepo;
    private final WithdrawalRepository withdrawalRepo;
    private final DisputeRepository disputeRepo;
    private final MessageRepository messageRepo;

    @Override
    public void run(String... args) {
        User admin = seed("demo_admin", "管理员", Role.ADMIN, Grade.C);
        User workerA = seed("demo_worker_a", "大神·A级", Role.WORKER, Grade.A);
        User workerB = seed("demo_worker_b", "高手·B级", Role.WORKER, Grade.B);
        User workerC = seed("demo_worker_c", "陪玩·C级", Role.WORKER, Grade.C);
        User client = seed("demo_client_1", "玩家小明", Role.CLIENT, Grade.C);
        decorateWorker(workerA, 82, 3000, "🦊");
        decorateWorker(workerB, 68, 2500, "🐯");
        decorateWorker(workerC, 55, 2000, "🐰");
        if (orderRepo.count() == 0) {
            seedDemoData(client, workerA, workerB, workerC);
        }
    }

    private User seed(String openid, String nickname, Role role, Grade grade) {
        if (userRepo.findByOpenid(openid).isPresent()) return userRepo.findByOpenid(openid).get();
        User u = new User();
        u.setOpenid(openid);
        u.setNickname(nickname);
        u.setRole(role);
        u.setGrade(grade);
        return userRepo.save(u);
    }

    private void decorateWorker(User worker, int winRate, int minPrice, String avatar) {
        worker.setWinRate(winRate);
        worker.setMinPrice(minPrice);
        worker.setAvatar(avatar);
        userRepo.save(worker);
    }

    private Order order(User buyer, Grade grade, String title, String category, int budget, int workers,
                        OrderStatus status, PaymentStatus paymentStatus) {
        Order o = new Order();
        o.setOrderNo("DEMO" + System.nanoTime());
        o.setBuyerId(buyer.getId());
        o.setGrade(grade);
        o.setCategory(category);
        o.setTitle(title);
        o.setDescription("演示订单：" + title);
        o.setBudget(budget);
        o.setRequiredWorkers(workers);
        o.setContact("13800000000");
        o.setStatus(status);
        o.setPaymentStatus(paymentStatus);
        return orderRepo.save(o);
    }

    private OrderAssignment assignment(Order o, User worker, AssignmentStatus status, boolean submitted) {
        OrderAssignment a = new OrderAssignment();
        a.setOrderId(o.getId());
        a.setWorkerId(worker.getId());
        a.setStatus(status);
        a.setAcceptedAt(LocalDateTime.now().minusHours(2));
        if (submitted) a.setSubmittedAt(LocalDateTime.now().minusHours(1));
        return assignmentRepo.save(a);
    }

    private void seedDemoData(User client, User workerA, User workerB, User workerC) {
        order(client, Grade.C, "王者荣耀 星耀陪练上分", "王者荣耀陪玩", 3000, 1, OrderStatus.PENDING_PAYMENT, PaymentStatus.PENDING);

        Order published = order(client, Grade.C, "英雄联盟 大神双排", "英雄联盟陪玩", 5000, 1, OrderStatus.PUBLISHED, PaymentStatus.PAID);
        Order accepted = order(client, Grade.B, "和平精英 四排带飞", "和平精英陪玩", 4000, 1, OrderStatus.ACCEPTED, PaymentStatus.PAID);
        assignment(accepted, workerB, AssignmentStatus.ACCEPTED, false);
        message(accepted, client, "今晚八点开黑可以吗？");
        message(accepted, workerB, "可以，我这边准备好了");

        Order delivered = order(client, Grade.C, "王者荣耀 巅峰赛陪打", "王者荣耀陪玩", 6000, 1, OrderStatus.DELIVERED, PaymentStatus.PAID);
        assignment(delivered, workerC, AssignmentStatus.ACCEPTED, true);

        Order refunded = order(client, Grade.B, "原神 世界任务互助", "其他游戏", 2000, 1, OrderStatus.REFUNDED, PaymentStatus.REFUNDED);

        Order completed = order(client, Grade.A, "英雄联盟 五黑车队", "组队开黑", 8000, 2, OrderStatus.COMPLETED, PaymentStatus.PAID);
        assignment(completed, workerA, AssignmentStatus.ACCEPTED, true);
        assignment(completed, workerB, AssignmentStatus.ACCEPTED, true);
        earning(completed, workerA, 4000, Earning.EarningStatus.SETTLED);
        earning(completed, workerB, 4000, Earning.EarningStatus.PENDING);
        review(completed, client, workerA, 5, "大神带飞，配合很好");
        message(completed, client, "感谢两位大神，体验很好");
        message(completed, workerA, "客气了，有需要再约");

        Order disputed = order(client, Grade.C, "王者荣耀 攻略教学", "攻略教学", 3500, 1, OrderStatus.DISPUTED, PaymentStatus.PAID);
        assignment(disputed, workerC, AssignmentStatus.ACCEPTED, true);
        Dispute d = new Dispute();
        d.setOrderId(disputed.getId());
        d.setCreatedBy(client.getId());
        d.setReason("教学时间需要调整");
        d.setPreviousStatus(OrderStatus.DELIVERED);
        disputeRepo.save(d);

        Order published2 = order(client, Grade.B, "和平精英 上分陪练", "和平精英陪玩", 4500, 2, OrderStatus.PUBLISHED, PaymentStatus.PAID);
        Order completedC = order(client, Grade.C, "王者荣耀 陪练教学", "攻略教学", 3000, 1, OrderStatus.COMPLETED, PaymentStatus.PAID);
        assignment(completedC, workerC, AssignmentStatus.ACCEPTED, true);
        earning(completedC, workerC, 3000, Earning.EarningStatus.PENDING);
        review(completedC, client, workerC, 4, "很耐心，讲解清楚");

        notification(workerB, "订单已完成", "演示订单已完成，收益已入账");
        notification(client, "订单有接单人", "演示订单已有人接下");

        Withdrawal w = new Withdrawal();
        w.setWorkerId(workerA.getId());
        w.setAmount(2000);
        w.setStatus(Withdrawal.WithdrawalStatus.PENDING);
        withdrawalRepo.save(w);
    }

    private void earning(Order o, User worker, int amount, Earning.EarningStatus status) {
        Earning e = new Earning();
        e.setOrderId(o.getId());
        e.setWorkerId(worker.getId());
        e.setAmount(amount);
        e.setStatus(status);
        earningRepo.save(e);
    }

    private void review(Order o, User from, User to, int rating, String comment) {
        Review r = new Review();
        r.setOrderId(o.getId());
        r.setFromId(from.getId());
        r.setToId(to.getId());
        r.setRating(rating);
        r.setComment(comment);
        reviewRepo.save(r);
    }

    private void notification(User to, String title, String body) {
        Notification n = new Notification();
        n.setUserId(to.getId());
        n.setTitle(title);
        n.setBody(body);
        notificationRepo.save(n);
    }

    private void message(Order o, User sender, String content) {
        Message m = new Message();
        m.setOrderId(o.getId());
        m.setSenderId(sender.getId());
        m.setContent(content);
        messageRepo.save(m);
    }
}
