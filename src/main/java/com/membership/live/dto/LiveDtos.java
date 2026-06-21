package com.membership.live.dto;

import com.membership.live.domain.EventStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class LiveDtos {

    private LiveDtos() { }

    public record CreateLiveEventRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 5_000) String description,
            @NotNull Instant startsAt,
            @NotNull UUID hostId) { }

    public record LiveEventResponse(
            UUID id,
            String title,
            String description,
            Instant startsAt,
            EventStatus status,
            UUID hostId,
            Instant createdAt,
            Instant updatedAt) { }
}
