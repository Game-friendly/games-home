package com.club.order.service;

import com.club.order.common.BizException;
import com.club.order.domain.Earning;
import com.club.order.domain.Withdrawal;
import com.club.order.repo.EarningRepository;
import com.club.order.repo.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final WithdrawalRepository withdrawalRepo;
    private final EarningRepository earningRepo;

    public int withdrawable(Long workerId) {
        int settled = earningRepo.findByWorkerId(workerId, Sort.unsorted()).stream()
                .filter(e -> e.getStatus() == Earning.EarningStatus.SETTLED)
                .mapToInt(Earning::getAmount).sum();
        int reserved = withdrawalRepo.findByWorkerId(workerId, Sort.unsorted()).stream()
                .filter(w -> w.getStatus() != Withdrawal.WithdrawalStatus.REJECTED)
                .mapToInt(Withdrawal::getAmount).sum();
        return Math.max(0, settled - reserved);
    }

    @Transactional
    public Withdrawal create(Long workerId, int amount) {
        if (amount <= 0) {
            throw new BizException(400, "提现金额必须大于 0");
        }
        int available = withdrawable(workerId);
        if (amount > available) {
            throw new BizException(400, "可提现余额不足");
        }
        Withdrawal w = new Withdrawal();
        w.setWorkerId(workerId);
        w.setAmount(amount);
        return withdrawalRepo.save(w);
    }

    public List<Withdrawal> mine(Long workerId) {
        return withdrawalRepo.findByWorkerId(workerId, Sort.by(Sort.Direction.DESC, "id"));
    }

    public List<Withdrawal> listAll() {
        return withdrawalRepo.findAllByOrderByIdDesc();
    }

    @Transactional
    public Withdrawal approve(Long id) {
        Withdrawal w = withdrawalRepo.findById(id).orElseThrow(() -> new BizException(404, "提现申请不存在"));
        if (w.getStatus() != Withdrawal.WithdrawalStatus.PENDING) {
            throw new BizException(400, "该申请已处理");
        }
        w.setStatus(Withdrawal.WithdrawalStatus.PAID);
        w.setHandledAt(LocalDateTime.now());
        return withdrawalRepo.save(w);
    }

    @Transactional
    public Withdrawal reject(Long id, String remark) {
        Withdrawal w = withdrawalRepo.findById(id).orElseThrow(() -> new BizException(404, "提现申请不存在"));
        if (w.getStatus() != Withdrawal.WithdrawalStatus.PENDING) {
            throw new BizException(400, "该申请已处理");
        }
        w.setStatus(Withdrawal.WithdrawalStatus.REJECTED);
        w.setHandledAt(LocalDateTime.now());
        w.setRemark(remark);
        return withdrawalRepo.save(w);
    }
}
