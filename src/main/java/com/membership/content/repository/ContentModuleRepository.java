package com.membership.content.repository;

import com.membership.content.domain.ContentModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContentModuleRepository extends JpaRepository<ContentModule, UUID> {
    List<ContentModule> findAllByOrderByPositionAsc();
}
