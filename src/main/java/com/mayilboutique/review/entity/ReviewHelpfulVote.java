package com.mayilboutique.review.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "review_helpful_votes")
public class ReviewHelpfulVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ReviewHelpfulVote() {
    }

    public ReviewHelpfulVote(Long reviewId, Long userId) {
        this.reviewId = reviewId;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public Long getReviewId() { return reviewId; }
    public Long getUserId() { return userId; }
}
