package com.membership.community.repository;

import com.membership.community.domain.Comment;
import com.membership.community.domain.ContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByPostIdAndStatusOrderByCreatedAtAsc(UUID postId, ContentStatus status);
    long countByPostIdAndStatus(UUID postId, ContentStatus status);
}
