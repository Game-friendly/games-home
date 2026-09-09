package com.club.order.repo;

import com.club.order.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByOrderIdOrderByCreatedAtAscIdAsc(Long orderId);
}
