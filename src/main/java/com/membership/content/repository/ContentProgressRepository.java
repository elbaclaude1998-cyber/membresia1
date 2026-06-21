package com.membership.content.repository;

import com.membership.content.domain.ContentProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentProgressRepository extends JpaRepository<ContentProgress, UUID> {
    Optional<ContentProgress> findByUserIdAndItemId(UUID userId, UUID itemId);
    List<ContentProgress> findByUserId(UUID userId);
}
