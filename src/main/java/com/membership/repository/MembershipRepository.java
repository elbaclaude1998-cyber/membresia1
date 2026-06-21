package com.membership.repository;

import com.membership.domain.Membership;
import com.membership.domain.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    Optional<Membership> findByUserId(UUID userId);

    boolean existsByUserIdAndStatus(UUID userId, MembershipStatus status);

    long countByStatus(MembershipStatus status);

    @Query("""
            select m from Membership m
            where m.autoRenew = true
              and m.status <> com.membership.domain.MembershipStatus.CANCELLED
              and m.endDate < :now
            """)
    List<Membership> findDueForAutoRenewal(@Param("now") Instant now);
}
