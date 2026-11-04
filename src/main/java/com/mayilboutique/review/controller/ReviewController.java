package com.mayilboutique.review.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mayilboutique.core.security.CustomUserDetails;
import com.mayilboutique.review.dto.HelpfulResponse;
import com.mayilboutique.review.dto.ReviewRequest;
import com.mayilboutique.review.dto.ReviewResponse;
import com.mayilboutique.review.dto.ReviewsResponse;
import com.mayilboutique.review.service.ReviewService;

import jakarta.validation.Valid;

@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** Public. When a customer token is sent, the response also says whether they may review and their own review. */
    @GetMapping("/api/products/public/{slug}/reviews")
    public ResponseEntity<ReviewsResponse> list(@PathVariable String slug,
                                                @RequestParam(required = false) String sort,
                                                @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(reviewService.list(slug, sort, principal == null ? null : principal.getUserId()));
    }

    @PostMapping("/api/customer/reviews")
    public ResponseEntity<ReviewResponse> create(@AuthenticationPrincipal CustomUserDetails principal,
                                                 @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.create(principal.getUserId(), request));
    }

    @PutMapping("/api/customer/reviews/{id}")
    public ResponseEntity<ReviewResponse> update(@AuthenticationPrincipal CustomUserDetails principal,
                                                 @PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.update(principal.getUserId(), id, request));
    }

    @DeleteMapping("/api/customer/reviews/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        reviewService.delete(principal.getUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/customer/reviews/{id}/helpful")
    public ResponseEntity<HelpfulResponse> helpful(@AuthenticationPrincipal CustomUserDetails principal,
                                                   @PathVariable Long id) {
        return ResponseEntity.ok(reviewService.toggleHelpful(principal.getUserId(), id));
    }
}
