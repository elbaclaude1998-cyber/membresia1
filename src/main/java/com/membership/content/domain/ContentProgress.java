package com.membership.content.domain;

import com.membership.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

/** Progreso (0-100) de un usuario sobre un ítem de contenido. */
@Entity
@Table(name = "content_progress",
        uniqueConstraints = @UniqueConstraint(name = "uq_progress", columnNames = {"user_id", "item_id"}))
public class ContentProgress extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ContentItem item;

    @Column(nullable = false)
    private int progress;

    @Column(nullable = false)
    private boolean completed;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public ContentItem getItem() { return item; }
    public void setItem(ContentItem item) { this.item = item; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
