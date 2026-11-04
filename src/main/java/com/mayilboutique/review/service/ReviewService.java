package com.mayilboutique.review.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mayilboutique.core.enums.OrderStatus;
import com.mayilboutique.core.enums.ProductStatus;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.order.entity.Order;
import com.mayilboutique.order.entity.OrderItem;
import com.mayilboutique.order.repository.OrderItemRepository;
import com.mayilboutique.order.repository.OrderRepository;
import com.mayilboutique.product.entity.Product;
import com.mayilboutique.product.repository.ProductRepository;
import com.mayilboutique.review.dto.HelpfulResponse;
import com.mayilboutique.review.dto.ReviewRequest;
import com.mayilboutique.review.dto.ReviewResponse;
import com.mayilboutique.review.dto.ReviewsResponse;
import com.mayilboutique.review.entity.ProductReview;
import com.mayilboutique.review.entity.ReviewHelpfulVote;
import com.mayilboutique.review.repository.ProductReviewRepository;
import com.mayilboutique.review.repository.ReviewHelpfulVoteRepository;

@Service
public class ReviewService {

    /** A customer may review once the product has actually reached them (a return request comes after delivery). */
    static final Set<OrderStatus> REVIEWABLE = Set.of(OrderStatus.DELIVERED, OrderStatus.RETURN_REQUESTED);
    private static final int MAX_RETURNED = 100;

    private final ProductReviewRepository reviews;
    private final ReviewHelpfulVoteRepository votes;
    private final ProductRepository products;
    private final OrderItemRepository orderItems;
    private final OrderRepository orders;

    public ReviewService(ProductReviewRepository reviews, ReviewHelpfulVoteRepository votes,
                         ProductRepository products, OrderItemRepository orderItems, OrderRepository orders) {
        this.reviews = reviews;
        this.votes = votes;
        this.products = products;
        this.orderItems = orderItems;
        this.orders = orders;
    }

    /** {@code userId} is null for a visitor who is not signed in as a customer. */
    @Transactional(readOnly = true)
    public ReviewsResponse list(String slug, String sort, Long userId) {
        Product product = publishedBySlug(slug);
        List<ProductReview> all = reviews.findByProductId(product.getId());

        long[] distribution = new long[5];
        long sum = 0;
        for (ProductReview r : all) {
            distribution[r.getRating() - 1]++;
            sum += r.getRating();
        }
        double average = all.isEmpty() ? 0 : Math.round(sum * 10.0 / all.size()) / 10.0;

        List<Long> ids = all.stream().map(ProductReview::getId).toList();
        Map<Long, Long> helpful = ids.isEmpty() ? Map.of() : votes.countByReviewIds(ids).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).longValue()));
        Set<Long> mineVoted = userId == null || ids.isEmpty() ? Set.of() : new HashSet<>(votes.findVotedReviewIds(userId, ids));

        Comparator<ProductReview> byNewest = Comparator.comparing(ProductReview::getCreatedAt).reversed();
        Comparator<ProductReview> order = switch (sort == null ? "helpful" : sort) {
            case "latest" -> byNewest;
            case "positive" -> Comparator.comparing(ProductReview::getRating).reversed().thenComparing(byNewest);
            case "negative" -> Comparator.comparing(ProductReview::getRating).thenComparing(byNewest);
            default -> Comparator.<ProductReview>comparingLong(r -> helpful.getOrDefault(r.getId(), 0L)).reversed()
                    .thenComparing(byNewest);
        };

        List<ReviewResponse> out = all.stream().sorted(order).limit(MAX_RETURNED)
                .map(r -> toResponse(r, helpful.getOrDefault(r.getId(), 0L), mineVoted.contains(r.getId()), r.getUserId().equals(userId)))
                .toList();

        ReviewResponse mine = null;
        boolean canReview = false;
        if (userId != null) {
            ProductReview existing = all.stream().filter(r -> r.getUserId().equals(userId)).findFirst().orElse(null);
            if (existing != null) {
                mine = toResponse(existing, helpful.getOrDefault(existing.getId(), 0L), false, true);
            } else {
                canReview = !orderItems.findPurchasedLines(userId, product.getId(), REVIEWABLE).isEmpty();
            }
        }
        return new ReviewsResponse(average, all.size(), distribution, out, canReview, mine);
    }

    @Transactional
    public ReviewResponse create(Long userId, ReviewRequest request) {
        Product product = products.findById(request.productId())
                .filter(p -> p.getStatus() == ProductStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Product not found"));
        List<OrderItem> purchased = orderItems.findPurchasedLines(userId, product.getId(), REVIEWABLE);
        if (purchased.isEmpty()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "You can review a product after it has been delivered to you");
        }
        if (reviews.findByUserIdAndProductId(userId, product.getId()).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "You have already reviewed this product");
        }
        OrderItem line = purchased.get(0);
        Order order = orders.findById(line.getOrderId()).orElse(null);
        ProductReview saved = reviews.save(new ProductReview(product.getId(), userId, request.rating(),
                clean(request.comment()), displayName(order), line.getColour(), line.getSize()));
        return toResponse(saved, 0, false, true);
    }

    @Transactional
    public ReviewResponse update(Long userId, Long reviewId, ReviewRequest request) {
        ProductReview review = ownReview(userId, reviewId);
        review.setRating(request.rating());
        review.setComment(clean(request.comment()));
        return toResponse(review, votes.countByReviewId(reviewId), false, true);
    }

    @Transactional
    public void delete(Long userId, Long reviewId) {
        reviews.delete(ownReview(userId, reviewId));
    }

    /** Marks / unmarks a review as helpful for this customer. Your own review cannot be voted on. */
    @Transactional
    public HelpfulResponse toggleHelpful(Long userId, Long reviewId) {
        ProductReview review = reviews.findById(reviewId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Review not found"));
        if (review.getUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "You cannot mark your own review as helpful");
        }
        var existing = votes.findByReviewIdAndUserId(reviewId, userId);
        boolean now;
        if (existing.isPresent()) {
            votes.delete(existing.get());
            now = false;
        } else {
            votes.save(new ReviewHelpfulVote(reviewId, userId));
            now = true;
        }
        votes.flush();
        return new HelpfulResponse(votes.countByReviewId(reviewId), now);
    }

    private ProductReview ownReview(Long userId, Long reviewId) {
        ProductReview review = reviews.findById(reviewId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Review not found"));
        if (!review.getUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Review not found");
        }
        return review;
    }

    private Product publishedBySlug(String slug) {
        return products.findBySlug(slug)
                .filter(p -> p.getStatus() == ProductStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private static String clean(String comment) {
        if (comment == null) {
            return null;
        }
        String trimmed = comment.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** "Rekha Panika" -> "Rekha P."; never exposes the mobile number. */
    static String displayName(Order order) {
        String name = order == null ? null : order.getShipName();
        if (name == null || name.isBlank()) {
            name = order == null ? null : order.getCustomerName();
        }
        if (name == null || name.isBlank() || name.matches(".*\\d.*")) {
            return "Verified customer";
        }
        String[] parts = name.trim().split("\\s+");
        String first = parts[0];
        return parts.length > 1 ? first + " " + Character.toUpperCase(parts[parts.length - 1].charAt(0)) + "." : first;
    }

    private static ReviewResponse toResponse(ProductReview r, long helpfulCount, boolean helpfulByMe, boolean mine) {
        return new ReviewResponse(r.getId(), r.getRating(), r.getComment(), r.getReviewerName(), r.getColour(),
                r.getSize(), r.getCreatedAt(), helpfulCount, helpfulByMe, mine);
    }
}
