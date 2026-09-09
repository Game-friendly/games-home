package com.club.order.repo;

import com.club.order.domain.Order;
import com.club.order.domain.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatusIn(Collection<OrderStatus> statuses, Sort sort);

    List<Order> findByStatus(OrderStatus status, Sort sort);

    List<Order> findByBuyerId(Long buyerId, Sort sort);

    /** 悲观写锁：保证抢单/接单/退单的原子性（同一订单串行处理）。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    /** 超时未接单的单（下单时设了截止时间）。 */
    List<Order> findByStatusAndDeadlineBefore(OrderStatus status, LocalDateTime now);

    /** 超时未确认的单（客户确认超时自动完成）。 */
    List<Order> findByStatusAndDeliveredAtBefore(OrderStatus status, LocalDateTime now);
}
