package com.club.order.repo;

import com.club.order.domain.Withdrawal;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {
    List<Withdrawal> findByWorkerId(Long workerId, Sort sort);
    List<Withdrawal> findAllByOrderByIdDesc();
}
