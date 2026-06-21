package com.membership.community.service;

import com.membership.community.domain.Comment;
import com.membership.community.domain.ContentStatus;
import com.membership.community.domain.Notification;
import com.membership.community.domain.NotificationType;
import com.membership.community.domain.Post;
import com.membership.community.domain.PostLike;
import com.membership.community.dto.CommunityDtos.CommentResponse;
import com.membership.community.dto.CommunityDtos.CreateCommentRequest;
import com.membership.community.dto.CommunityDtos.CreatePostRequest;
import com.membership.community.dto.CommunityDtos.LikeResponse;
import com.membership.community.dto.CommunityDtos.NotificationResponse;
import com.membership.community.dto.CommunityDtos.PostDetailResponse;
import com.membership.community.dto.CommunityDtos.PostResponse;
import com.membership.community.mapper.CommunityMapper;
import com.membership.community.repository.CommentRepository;
import com.membership.community.repository.LikeRepository;
import com.membership.community.repository.NotificationRepository;
import com.membership.community.repository.PostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class CommunityService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final NotificationRepository notificationRepository;
    private final ModerationService moderationService;
    private final CommunityMapper mapper;

    public CommunityService(PostRepository postRepository,
                            CommentRepository commentRepository,
                            LikeRepository likeRepository,
                            NotificationRepository notificationRepository,
                            ModerationService moderationService,
                            CommunityMapper mapper) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.notificationRepository = notificationRepository;
        this.moderationService = moderationService;
        this.mapper = mapper;
    }

    // ---- Posts ----

    @Transactional
    public PostResponse createPost(CreatePostRequest req) {
        Post post = new Post();
        post.setAuthorId(req.authorId());
        post.setTitle(req.title());
        post.setContent(req.content());
        post.setStatus(moderationService.screen(req.title() + " " + req.content()));
        postRepository.save(post);
        return mapper.toResponse(post, 0L);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> listPosts() {
        return postRepository.findByStatusOrderByCreatedAtDesc(ContentStatus.VISIBLE).stream()
                .map(p -> mapper.toResponse(p, commentRepository.countByPostIdAndStatus(p.getId(), ContentStatus.VISIBLE)))
                .toList();
    }

    /** Listado completo (todos los estados) para administración/moderación. */
    @Transactional(readOnly = true)
    public List<PostResponse> listAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(p -> mapper.toResponse(p, commentRepository.countByPostIdAndStatus(p.getId(), ContentStatus.VISIBLE)))
                .toList();
    }

    @Transactional(readOnly = true)
    public PostDetailResponse getPost(UUID postId) {
        Post post = loadVisible(postId);
        List<CommentResponse> comments = mapper.toCommentResponses(
                commentRepository.findByPostIdAndStatusOrderByCreatedAtAsc(postId, ContentStatus.VISIBLE));
        return new PostDetailResponse(mapper.toResponse(post, comments.size()), comments);
    }

    // ---- Comments ----

    @Transactional
    public CommentResponse addComment(UUID postId, CreateCommentRequest req) {
        Post post = loadVisible(postId);
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthorId(req.authorId());
        comment.setContent(req.content());
        comment.setStatus(moderationService.screen(req.content()));
        commentRepository.save(comment);

        if (!req.authorId().equals(post.getAuthorId())) {
            notify(post.getAuthorId(), NotificationType.NEW_COMMENT, "Nuevo comentario en tu publicación", post.getId());
        }
        return mapper.toResponse(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(UUID postId) {
        loadVisible(postId);
        return mapper.toCommentResponses(
                commentRepository.findByPostIdAndStatusOrderByCreatedAtAsc(postId, ContentStatus.VISIBLE));
    }

    // ---- Likes ----

    @Transactional
    public LikeResponse like(UUID postId, UUID userId) {
        Post post = loadVisible(postId);
        if (!likeRepository.existsByPostIdAndUserId(postId, userId)) {
            PostLike like = new PostLike();
            like.setPost(post);
            like.setUserId(userId);
            likeRepository.save(like);
            post.setLikeCount(post.getLikeCount() + 1);
            if (!userId.equals(post.getAuthorId())) {
                notify(post.getAuthorId(), NotificationType.NEW_LIKE, "A alguien le gustó tu publicación", post.getId());
            }
        }
        return new LikeResponse(postId, true, post.getLikeCount());
    }

    @Transactional
    public LikeResponse unlike(UUID postId, UUID userId) {
        Post post = load(postId);
        if (likeRepository.existsByPostIdAndUserId(postId, userId)) {
            likeRepository.deleteByPostIdAndUserId(postId, userId);
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        }
        return new LikeResponse(postId, false, post.getLikeCount());
    }

    // ---- Moderación ----

    @Transactional
    public PostResponse moderate(UUID postId, ContentStatus status) {
        Post post = load(postId);
        post.setStatus(status);
        if (status == ContentStatus.HIDDEN) {
            notify(post.getAuthorId(), NotificationType.CONTENT_MODERATED,
                    "Tu publicación fue ocultada por moderación", post.getId());
        }
        long count = commentRepository.countByPostIdAndStatus(postId, ContentStatus.VISIBLE);
        return mapper.toResponse(post, count);
    }

    // ---- Notifications ----

    @Transactional(readOnly = true)
    public List<NotificationResponse> notifications(UUID recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public void markRead(UUID notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        n.setRead(true);
    }

    // ---- internos ----

    private void notify(UUID recipientId, NotificationType type, String message, UUID postId) {
        if (recipientId == null) {
            return;
        }
        Notification n = new Notification();
        n.setRecipientId(recipientId);
        n.setType(type);
        n.setMessage(message);
        n.setPostId(postId);
        notificationRepository.save(n);
    }

    private Post load(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    /** Carga un post que no esté oculto por moderación. */
    private Post loadVisible(UUID postId) {
        Post post = load(postId);
        if (post.getStatus() == ContentStatus.HIDDEN) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }
        return post;
    }
}
