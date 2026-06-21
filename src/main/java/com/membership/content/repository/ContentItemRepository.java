package com.membership.content.repository;

import com.membership.content.domain.ContentItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContentItemRepository extends JpaRepository<ContentItem, UUID> {
    List<ContentItem> findByModuleIdOrderByPositionAsc(UUID moduleId);
    long countByModuleId(UUID moduleId);
}
