package com.membership.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AdminDtos {

    private AdminDtos() { }

    public record AdminUserResponse(
            UUID id,
            String email,
            String fullName,
            boolean enabled,
            List<String> roles,
            Instant createdAt) { }

    public record AdminMembershipResponse(
            UUID id,
            UUID userId,
            String plan,
            String status,
            boolean autoRenew,
            Instant endDate) { }

    public record StatsResponse(
            long users,
            long activeMemberships,
            long posts,
            long liveEvents,
            long contentModules,
            long contentItems) { }
}
