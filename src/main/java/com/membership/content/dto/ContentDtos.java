package com.membership.content.dto;

import com.membership.content.domain.ContentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ContentDtos {

    private ContentDtos() { }

    public record ModuleResponse(
            UUID id,
            String title,
            String description,
            int position,
            long itemCount,
            Instant createdAt) { }

    public record ItemResponse(
            UUID id,
            UUID moduleId,
            String title,
            ContentType type,
            String url,
            int durationSeconds,
            int position,
            int progress,
            boolean completed) { }

    public record ModuleDetailResponse(
            ModuleResponse module,
            List<ItemResponse> items) { }

    public record ProgressRequest(
            @Min(0) @Max(100) int progress) { }
}
