package com.membership.community.repository;

import com.membership.community.domain.ContentStatus;
import com.membership.community.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
    List<Post> findByStatusOrderByCreatedAtDesc(ContentStatus status);
    List<Post> findAllByOrderByCreatedAtDesc();
}
