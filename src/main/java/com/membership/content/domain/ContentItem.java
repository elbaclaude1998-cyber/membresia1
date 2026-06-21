package com.membership.content.domain;

import com.membership.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "content_items")
public class ContentItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private ContentModule module;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ContentType type;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "position", nullable = false)
    private int position;

    public ContentModule getModule() { return module; }
    public void setModule(ContentModule module) { this.module = module; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ContentType getType() { return type; }
    public void setType(ContentType type) { this.type = type; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
