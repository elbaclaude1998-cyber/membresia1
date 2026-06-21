package com.membership.community.dto;

import com.membership.community.domain.ContentStatus;
import com.membership.community.domain.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CommunityDtos {

    private CommunityDtos() { }

    // ---- Posts ----
    public record CreatePostRequest(
            @NotNull UUID authorId,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 10_000) String content) { }

    public record PostResponse(
            UUID id,
            UUID authorId,
            String title,
            String content,
            ContentStatus status,
            long likeCount,
            long commentCount,
            Instant createdAt,
            Instant updatedAt) { }

    public record PostDetailResponse(
            PostResponse post,
            List<CommentResponse> comments) { }

    // ---- Comments ----
    public record CreateCommentRequest(
            @NotNull UUID authorId,
            @NotBlank @Size(max = 5_000) String content) { }

    public record CommentResponse(
            UUID id,
            UUID postId,
            UUID authorId,
            String content,
            ContentStatus status,
            Instant createdAt) { }

    // ---- Likes ----
    public record LikeRequest(
            @NotNull UUID userId) { }

    public record LikeResponse(
            UUID postId,
            boolean liked,
            long likeCount) { }

    // ---- Moderación ----
    public record ModerationRequest(
            @NotNull ContentStatus status) { }

    // ---- Notifications ----
    public record NotificationResponse(
            UUID id,
            UUID recipientId,
            NotificationType type,
            String message,
            UUID postId,
            boolean read,
            Instant createdAt) { }
}
