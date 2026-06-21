package com.membership.community.domain;

import com.membership.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

/**
 * "Like" de un usuario sobre un post. Se llama PostLike (no Like) para evitar
 * colisión con la palabra reservada LIKE de HQL/SQL en las consultas derivadas.
 */
@Entity
@Table(name = "likes", uniqueConstraints = @UniqueConstraint(name = "uq_like", columnNames = {"post_id", "user_id"}))
public class PostLike extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
}
