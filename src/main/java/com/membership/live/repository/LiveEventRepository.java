package com.membership.live.repository;

import com.membership.live.domain.EventStatus;
import com.membership.live.domain.LiveEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LiveEventRepository extends JpaRepository<LiveEvent, UUID> {
    List<LiveEvent> findAllByOrderByStartsAtDesc();
    List<LiveEvent> findByStatusOrderByStartsAtAsc(EventStatus status);
}
