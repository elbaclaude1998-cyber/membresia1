package com.membership.community.repository;

import com.membership.community.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LikeRepository extends JpaRepository<PostLike, UUID> {
    boolean existsByPostIdAndUserId(UUID postId, UUID userId);
    long deleteByPostIdAndUserId(UUID postId, UUID userId);
}
