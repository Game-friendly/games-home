package com.club.order.repo;

import com.club.order.domain.Earning;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EarningRepository extends JpaRepository<Earning, Long> {
    List<Earning> findByWorkerId(Long workerId, Sort sort);
    List<Earning> findByOrderId(Long orderId);
    Optional<Earning> findByOrderIdAndWorkerId(Long orderId, Long workerId);
}
