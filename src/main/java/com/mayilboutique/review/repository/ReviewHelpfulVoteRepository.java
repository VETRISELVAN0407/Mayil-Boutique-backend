package com.mayilboutique.review.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mayilboutique.review.entity.ReviewHelpfulVote;

public interface ReviewHelpfulVoteRepository extends JpaRepository<ReviewHelpfulVote, Long> {

    Optional<ReviewHelpfulVote> findByReviewIdAndUserId(Long reviewId, Long userId);

    long countByReviewId(Long reviewId);

    /** Rows of {@code [reviewId, votes]}. */
    @Query("SELECT v.reviewId, COUNT(v) FROM ReviewHelpfulVote v WHERE v.reviewId IN :ids GROUP BY v.reviewId")
    List<Object[]> countByReviewIds(@Param("ids") Collection<Long> ids);

    @Query("SELECT v.reviewId FROM ReviewHelpfulVote v WHERE v.userId = :userId AND v.reviewId IN :ids")
    List<Long> findVotedReviewIds(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);
}
