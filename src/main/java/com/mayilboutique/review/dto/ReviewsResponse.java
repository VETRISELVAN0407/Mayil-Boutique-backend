package com.mayilboutique.review.dto;

import java.util.List;

/**
 * Reviews of one product. {@code distribution[i]} is the number of {@code i+1}-star ratings.
 * {@code canReview} is true only for a signed-in customer with a delivered order of the product who has not
 * reviewed it yet; {@code myReview} is that customer's own review, if any.
 */
public record ReviewsResponse(
        double average,
        long count,
        long[] distribution,
        List<ReviewResponse> reviews,
        boolean canReview,
        ReviewResponse myReview) {
}
