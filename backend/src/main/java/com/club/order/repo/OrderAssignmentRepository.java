package com.club.order.repo;

import com.club.order.domain.AssignmentStatus;
import com.club.order.domain.OrderAssignment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderAssignmentRepository extends JpaRepository<OrderAssignment, Long> {

    List<OrderAssignment> findByOrderIdOrderByIdAsc(Long orderId);

    List<OrderAssignment> findByOrderIdAndStatusIn(Long orderId, Collection<AssignmentStatus> statuses);

    List<OrderAssignment> findByWorkerIdAndStatusIn(Long workerId, Collection<AssignmentStatus> statuses);

    List<OrderAssignment> findByWorkerId(Long workerId);

    Optional<OrderAssignment> findByOrderIdAndWorkerIdAndStatusIn(
            Long orderId, Long workerId, Collection<AssignmentStatus> statuses);

    List<OrderAssignment> findByStatusAndReserveExpiresAtBefore(
            AssignmentStatus status, LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from OrderAssignment a where a.id = :id")
    Optional<OrderAssignment> findByIdForUpdate(@Param("id") Long id);
}
