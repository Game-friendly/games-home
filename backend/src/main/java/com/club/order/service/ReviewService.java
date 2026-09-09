package com.club.order.service;

import com.club.order.common.BizException;
import com.club.order.domain.Order;
import com.club.order.domain.Review;
import com.club.order.repo.OrderRepository;
import com.club.order.repo.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final OrderRepository orderRepo;

    /** 客户在订单完成后评价某个接单人。多人单可对每位接单人分别评价一次。 */
    @Transactional
    public Review create(Long orderId, Long fromId, Long toId, int rating, String comment) {
        Order o = orderRepo.findById(orderId).orElseThrow(() -> new BizException(404, "订单不存在"));
        if (!fromId.equals(o.getBuyerId())) {
            throw new BizException(403, "只有发单客户能评价");
        }
        if (o.getStatus() != com.club.order.domain.OrderStatus.COMPLETED) {
            throw new BizException(400, "订单完成后才能评价");
        }
        if (rating < 1 || rating > 5) {
            throw new BizException(400, "评分须为 1-5");
        }
        if (reviewRepo.findByOrderIdAndToId(orderId, toId).isPresent()) {
            throw new BizException(400, "该接单人已被评价过");
        }
        Review r = new Review();
        r.setOrderId(orderId);
        r.setFromId(fromId);
        r.setToId(toId);
        r.setRating(rating);
        r.setComment(comment);
        return reviewRepo.save(r);
    }

    public List<Review> byWorker(Long workerId) {
        return reviewRepo.findByToId(workerId);
    }

    public List<Review> byOrder(Long orderId) {
        return reviewRepo.findByOrderId(orderId);
    }
}
