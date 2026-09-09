package com.club.order.repo;

import com.club.order.domain.AcceptLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcceptLogRepository extends JpaRepository<AcceptLog, Long> {
}
