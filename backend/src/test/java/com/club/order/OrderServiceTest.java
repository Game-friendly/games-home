package com.club.order;

import com.club.order.common.BizException;
import com.club.order.domain.*;
import com.club.order.repo.EarningRepository;
import com.club.order.repo.OrderAssignmentRepository;
import com.club.order.repo.OrderRepository;
import com.club.order.repo.UserRepository;
import com.club.order.service.OrderService;
import com.club.order.service.ReviewService;
import com.club.order.web.dto.CreateOrderRequest;
import com.club.order.web.dto.OrderDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

/**
 * 核心业务规则测试：等级门槛、压制抢单、同级禁止、退单扣分、固化、并发、收益、评价、多人单。
 */
@SpringBootTest
class OrderServiceTest {

    @Autowired OrderService orderService;
    @Autowired ReviewService reviewService;
    @Autowired UserRepository userRepo;
    @Autowired OrderRepository orderRepo;
    @Autowired EarningRepository earningRepo;
    @Autowired OrderAssignmentRepository assignmentRepo;

    private User user(String openid, Role role, Grade grade) {
        User u = new User();
        u.setOpenid(openid);
        u.setNickname(openid);
        u.setRole(role);
        u.setGrade(grade);
        return userRepo.save(u);
    }

    private OrderDTO createOrder(Long buyerId, Grade grade, int budget, LocalDateTime deadline) {
        return createOrder(buyerId, grade, budget, deadline, 1);
    }

    private OrderDTO createOrder(Long buyerId, Grade grade, int budget, LocalDateTime deadline, int requiredWorkers) {
        OrderDTO created = orderService.create(new CreateOrderRequest(
                "测试单-" + System.nanoTime(), "王者荣耀陪玩", grade.name(), "desc", budget, requiredWorkers,
                deadline, "13000000000", null, null, null, null), buyerId);
        return orderService.markPaid(created.id(), buyerId);
    }

    /** 把某单所有保护期改成过去并跑固化，等价于「等 2 分钟」。 */
    private void fastForward(OrderDTO dto) {
        for (OrderAssignment a : assignmentRepo.findByOrderIdOrderByIdAsc(dto.id())) {
            if (a.getStatus() == AssignmentStatus.RESERVED) {
                a.setReserveExpiresAt(LocalDateTime.now().minusSeconds(1));
                assignmentRepo.save(a);
            }
        }
        orderService.finalizeExpired();
    }

    @Test
    void cWorkerCannotFirstAcceptAOrder() {
        User client = user("t_client_a", Role.CLIENT, Grade.C);
        User c = user("t_worker_c_a", Role.WORKER, Grade.C);
        OrderDTO dto = createOrder(client.getId(), Grade.A, 5000, null);
        assertThatThrownBy(() -> orderService.accept(dto.id(), c.getId()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("等级不足");
    }

    @Test
    void bWorkerCanFirstAcceptAOrder() {
        User client = user("t_client_b", Role.CLIENT, Grade.C);
        User b = user("t_worker_b_b", Role.WORKER, Grade.B);
        OrderDTO dto = createOrder(client.getId(), Grade.A, 5000, null);
        OrderDTO accepted = orderService.accept(dto.id(), b.getId());
        assertThat(accepted.status()).isEqualTo("RESERVED");
        assertThat(accepted.workers()).hasSize(1);
        assertThat(accepted.workers().get(0).workerId()).isEqualTo(b.getId());
        assertThat(accepted.workers().get(0).reserveExpiresAt()).isNotNull();
    }

    @Test
    void lowerGradeCannotStealButHigherCan() {
        User client = user("t_client_c", Role.CLIENT, Grade.C);
        User a = user("t_worker_a_c", Role.WORKER, Grade.A);
        User b = user("t_worker_b_c", Role.WORKER, Grade.B);
        User c = user("t_worker_c_c", Role.WORKER, Grade.C);
        OrderDTO dto = createOrder(client.getId(), Grade.C, 1000, null);

        orderService.accept(dto.id(), b.getId());
        assertThatThrownBy(() -> orderService.steal(dto.id(), c.getId(), b.getId()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("等级不足");

        OrderDTO after = orderService.steal(dto.id(), a.getId(), b.getId());
        assertThat(after.workers().get(0).workerId()).isEqualTo(a.getId());
    }

    @Test
    void stealByHigherGradeResetsTwoMinuteWindow() {
        User client = user("t_client_d", Role.CLIENT, Grade.C);
        User b = user("t_worker_b_d", Role.WORKER, Grade.B);
        User c = user("t_worker_c_d", Role.WORKER, Grade.C);
        OrderDTO dto = createOrder(client.getId(), Grade.C, 1000, null);

        OrderDTO first = orderService.accept(dto.id(), c.getId());
        OrderDTO afterSteal = orderService.steal(dto.id(), b.getId(), c.getId());
        assertThat(afterSteal.workers().get(0).workerId()).isEqualTo(b.getId());
        assertThat(afterSteal.workers().get(0).reserveExpiresAt())
                .isAfter(first.workers().get(0).reserveExpiresAt());
    }

    @Test
    void sameGradeCannotSteal() {
        User client = user("t_client_e", Role.CLIENT, Grade.C);
        User b1 = user("t_worker_b1_e", Role.WORKER, Grade.B);
        User b2 = user("t_worker_b2_e", Role.WORKER, Grade.B);
        OrderDTO dto = createOrder(client.getId(), Grade.B, 2000, null);

        orderService.accept(dto.id(), b1.getId());
        assertThatThrownBy(() -> orderService.steal(dto.id(), b2.getId(), b1.getId()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("等级不足");
    }

    @Test
    void withdrawInReservedReturnsToPoolWithoutPenalty() {
        User client = user("t_client_f", Role.CLIENT, Grade.C);
        User b = user("t_worker_b_f", Role.WORKER, Grade.B);
        OrderDTO dto = createOrder(client.getId(), Grade.C, 1000, null);
        orderService.accept(dto.id(), b.getId());

        OrderDTO after = orderService.withdraw(dto.id(), b.getId());
        assertThat(after.status()).isEqualTo("PUBLISHED");
        assertThat(after.workers()).isEmpty();
        assertThat(userRepo.findById(b.getId()).orElseThrow().getCreditScore()).isEqualTo(100);
    }

    @Test
    void withdrawInAcceptedDeductsCredit() {
        User client = user("t_client_g", Role.CLIENT, Grade.C);
        User b = user("t_worker_b_g", Role.WORKER, Grade.B);
        OrderDTO dto = createOrder(client.getId(), Grade.C, 1000, null);
        orderService.accept(dto.id(), b.getId());
        fastForward(dto);
        assertThat(orderRepo.findById(dto.id()).orElseThrow().getStatus()).isEqualTo(OrderStatus.ACCEPTED);

        orderService.withdraw(dto.id(), b.getId());
        assertThat(userRepo.findById(b.getId()).orElseThrow().getCreditScore()).isEqualTo(95);
        assertThat(orderRepo.findById(dto.id()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PUBLISHED);
    }

    @Test
    void finalizeExpiredReservedToAccepted() {
        User client = user("t_client_h", Role.CLIENT, Grade.C);
        User b = user("t_worker_b_h", Role.WORKER, Grade.B);
        OrderDTO dto = createOrder(client.getId(), Grade.C, 1000, null);
        orderService.accept(dto.id(), b.getId());
        fastForward(dto);

        assertThat(orderRepo.findById(dto.id()).orElseThrow().getStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test
    void concurrentAcceptOnlyOneWins() throws Exception {
        User client = user("t_client_i", Role.CLIENT, Grade.C);
        User b1 = user("t_worker_b1_i", Role.WORKER, Grade.B);
        User b2 = user("t_worker_b2_i", Role.WORKER, Grade.B);
        OrderDTO dto = createOrder(client.getId(), Grade.C, 1000, null);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Callable<String>> tasks = List.of(
                () -> { try { orderService.accept(dto.id(), b1.getId()); return "OK"; }
                        catch (BizException e) { return "FAIL:" + e.getCode(); } },
                () -> { try { orderService.accept(dto.id(), b2.getId()); return "OK"; }
                        catch (BizException e) { return "FAIL:" + e.getCode(); } }
        );
        List<Future<String>> results = pool.invokeAll(tasks);
        pool.shutdown();

        List<String> outcomes = new ArrayList<>();
        for (Future<String> f : results) outcomes.add(f.get());
        assertThat(outcomes).containsExactlyInAnyOrder("OK", "FAIL:400");
        assertThat(orderRepo.findById(dto.id()).orElseThrow().getStatus()).isEqualTo(OrderStatus.RESERVED);
    }

    @Test
    void completedFlowCreditsEarning() {
        User client = user("t_client_j", Role.CLIENT, Grade.C);
        User b = user("t_worker_b_j", Role.WORKER, Grade.B);
        OrderDTO dto = createOrder(client.getId(), Grade.C, 5000, null);

        orderService.accept(dto.id(), b.getId());
        fastForward(dto);
        orderService.complete(dto.id(), b.getId());
        orderService.confirm(dto.id(), client.getId());

        assertThat(orderRepo.findById(dto.id()).orElseThrow().getStatus()).isEqualTo(OrderStatus.COMPLETED);
        Earning e = earningRepo.findByOrderIdAndWorkerId(dto.id(), b.getId()).orElseThrow();
        assertThat(e.getAmount()).isEqualTo(5000);
        assertThat(e.getStatus()).isEqualTo(Earning.EarningStatus.PENDING);
    }

    @Test
    void reviewOnlyAfterCompletedAndOnce() {
        User client = user("t_client_k", Role.CLIENT, Grade.C);
        User b = user("t_worker_b_k", Role.WORKER, Grade.B);
        OrderDTO dto = createOrder(client.getId(), Grade.C, 1000, null);
        orderService.accept(dto.id(), b.getId());
        fastForward(dto);
        orderService.complete(dto.id(), b.getId());

        assertThatThrownBy(() -> reviewService.create(dto.id(), client.getId(), b.getId(), 5, "好"))
                .isInstanceOf(BizException.class);

        orderService.confirm(dto.id(), client.getId());
        Review r = reviewService.create(dto.id(), client.getId(), b.getId(), 5, "很好");
        assertThat(r.getRating()).isEqualTo(5);
        assertThatThrownBy(() -> reviewService.create(dto.id(), client.getId(), b.getId(), 4, "再来一次"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已被评价过");
    }

    @Test
    void twoPersonOrderKeepsTwoSlotsAndSplitsEarning() {
        User client = user("t_client_l", Role.CLIENT, Grade.C);
        User b = user("t_worker_b_l", Role.WORKER, Grade.B);
        User c = user("t_worker_c_l", Role.WORKER, Grade.C);
        OrderDTO dto = createOrder(client.getId(), Grade.C, 3000, null, 2);

        OrderDTO first = orderService.accept(dto.id(), b.getId());
        assertThat(first.status()).isEqualTo("PUBLISHED");
        assertThat(first.workers()).hasSize(1);

        // 同一个人不能占两个位置
        assertThatThrownBy(() -> orderService.accept(dto.id(), b.getId()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已接下该单");

        OrderDTO second = orderService.accept(dto.id(), c.getId());
        assertThat(second.status()).isEqualTo("RESERVED");
        assertThat(second.workers()).hasSize(2);

        fastForward(second);
        assertThat(orderRepo.findById(dto.id()).orElseThrow().getStatus()).isEqualTo(OrderStatus.ACCEPTED);

        orderService.complete(dto.id(), b.getId());
        orderService.complete(dto.id(), c.getId());
        orderService.confirm(dto.id(), client.getId());

        assertThat(orderRepo.findById(dto.id()).orElseThrow().getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(earningRepo.findByOrderIdAndWorkerId(dto.id(), b.getId()).orElseThrow().getAmount()).isEqualTo(1500);
        assertThat(earningRepo.findByOrderIdAndWorkerId(dto.id(), c.getId()).orElseThrow().getAmount()).isEqualTo(1500);
    }
}
