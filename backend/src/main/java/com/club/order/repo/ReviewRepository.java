package com.club.order.repo;

import com.club.order.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByOrderId(Long orderId);
    Optional<Review> findByOrderIdAndToId(Long orderId, Long toId);
    List<Review> findByToId(Long toId);
}
