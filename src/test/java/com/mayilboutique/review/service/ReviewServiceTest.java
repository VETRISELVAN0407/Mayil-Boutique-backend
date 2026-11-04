package com.mayilboutique.review.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.mayilboutique.core.enums.ProductStatus;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.order.entity.Order;
import com.mayilboutique.order.entity.OrderItem;
import com.mayilboutique.order.repository.OrderItemRepository;
import com.mayilboutique.order.repository.OrderRepository;
import com.mayilboutique.product.entity.Product;
import com.mayilboutique.product.repository.ProductRepository;
import com.mayilboutique.review.dto.ReviewRequest;
import com.mayilboutique.review.dto.ReviewsResponse;
import com.mayilboutique.review.entity.ProductReview;
import com.mayilboutique.review.entity.ReviewHelpfulVote;
import com.mayilboutique.review.repository.ProductReviewRepository;
import com.mayilboutique.review.repository.ReviewHelpfulVoteRepository;

class ReviewServiceTest {

    private static final Long USER = 5L;
    private static final Long PRODUCT = 9L;

    private ProductReviewRepository reviews;
    private ReviewHelpfulVoteRepository votes;
    private ProductRepository products;
    private OrderItemRepository orderItems;
    private OrderRepository orders;
    private ReviewService service;

    @BeforeEach
    void setUp() {
        reviews = mock(ProductReviewRepository.class);
        votes = mock(ReviewHelpfulVoteRepository.class);
        products = mock(ProductRepository.class);
        orderItems = mock(OrderItemRepository.class);
        orders = mock(OrderRepository.class);
        service = new ReviewService(reviews, votes, products, orderItems, orders);

        Product product = mock(Product.class);
        when(product.getId()).thenReturn(PRODUCT);
        when(product.getStatus()).thenReturn(ProductStatus.PUBLISHED);
        when(products.findById(PRODUCT)).thenReturn(Optional.of(product));
        when(products.findBySlug("saree")).thenReturn(Optional.of(product));
        when(reviews.save(any(ProductReview.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void cannotReviewWithoutADeliveredOrder() {
        when(orderItems.findPurchasedLines(USER, PRODUCT, ReviewService.REVIEWABLE)).thenReturn(List.of());

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.create(USER, new ReviewRequest(PRODUCT, 5, "Lovely")));

        assertEquals(HttpStatus.FORBIDDEN, e.getHttpStatus());
        verify(reviews, never()).save(any());
    }

    @Test
    void secondReviewOfTheSameProductIsRejected() {
        when(orderItems.findPurchasedLines(USER, PRODUCT, ReviewService.REVIEWABLE)).thenReturn(List.of(new OrderItem()));
        when(reviews.findByUserIdAndProductId(USER, PRODUCT)).thenReturn(Optional.of(new ProductReview()));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.create(USER, new ReviewRequest(PRODUCT, 4, null)));

        assertEquals(HttpStatus.CONFLICT, e.getHttpStatus());
    }

    @Test
    void verifiedBuyerReviewKeepsColourSizeAndShortName() {
        OrderItem line = new OrderItem();
        ReflectionTestUtils.setField(line, "orderId", 11L);
        ReflectionTestUtils.setField(line, "colour", "Maroon");
        ReflectionTestUtils.setField(line, "size", "S");
        Order order = new Order();
        order.setShipName("Rekha Panika");
        when(orderItems.findPurchasedLines(USER, PRODUCT, ReviewService.REVIEWABLE)).thenReturn(List.of(line));
        when(orders.findById(11L)).thenReturn(Optional.of(order));
        when(reviews.findByUserIdAndProductId(USER, PRODUCT)).thenReturn(Optional.empty());

        var saved = service.create(USER, new ReviewRequest(PRODUCT, 3, "  Fabric is good  "));

        assertEquals("Rekha P.", saved.reviewerName());
        assertEquals("Maroon", saved.colour());
        assertEquals("S", saved.size());
        assertEquals("Fabric is good", saved.comment());
        assertTrue(saved.mine());
    }

    @Test
    void nameWithDigitsOrBlankNeverLeaksAsDisplayName() {
        Order order = new Order();
        order.setShipName("9876543210");
        assertEquals("Verified customer", ReviewService.displayName(order));
        assertEquals("Verified customer", ReviewService.displayName(null));
    }

    @Test
    void summaryCountsStarsAndAverage() {
        ProductReview a = review(1L, 5);
        ProductReview b = review(2L, 4);
        ProductReview c = review(3L, 4);
        when(reviews.findByProductId(PRODUCT)).thenReturn(List.of(a, b, c));
        when(votes.countByReviewIds(any())).thenReturn(List.<Object[]>of(new Object[] {2L, 3L}));

        ReviewsResponse out = service.list("saree", "helpful", null);

        assertEquals(3, out.count());
        assertEquals(4.3, out.average());
        assertEquals(2, out.distribution()[3]);
        assertEquals(2L, out.reviews().get(0).id(), "most helpful first");
        assertEquals(false, out.canReview());
    }

    @Test
    void ownReviewCannotBeVotedHelpful() {
        ProductReview own = review(1L, 5);
        when(reviews.findById(1L)).thenReturn(Optional.of(own));

        BusinessException e = assertThrows(BusinessException.class, () -> service.toggleHelpful(USER, 1L));

        assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus());
        verify(votes, never()).save(any(ReviewHelpfulVote.class));
    }

    private static ProductReview review(Long id, int rating) {
        ProductReview r = new ProductReview(PRODUCT, id == 1L ? USER : 100 + id, rating, "ok", "Name", null, null);
        ReflectionTestUtils.setField(r, "id", id);
        ReflectionTestUtils.setField(r, "createdAt", java.time.Instant.now().minusSeconds(100 - id));
        return r;
    }
}
