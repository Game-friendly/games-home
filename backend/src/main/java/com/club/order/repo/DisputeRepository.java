package com.club.order.repo;

import com.club.order.domain.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    Optional<Dispute> findByOrderIdAndStatus(Long orderId, Dispute.DisputeStatus status);
    List<Dispute> findAllByOrderByIdDesc();
}
