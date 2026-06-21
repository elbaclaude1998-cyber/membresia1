package com.membership.community.web;

import com.membership.community.dto.CommunityDtos.CommentResponse;
import com.membership.community.dto.CommunityDtos.CreateCommentRequest;
import com.membership.community.dto.CommunityDtos.CreatePostRequest;
import com.membership.community.dto.CommunityDtos.LikeRequest;
import com.membership.community.dto.CommunityDtos.LikeResponse;
import com.membership.community.dto.CommunityDtos.ModerationRequest;
import com.membership.community.dto.CommunityDtos.PostDetailResponse;
import com.membership.community.dto.CommunityDtos.PostResponse;
import com.membership.community.service.CommunityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/posts")
public class PostController {

    private final CommunityService communityService;

    public PostController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @PostMapping
    public ResponseEntity<PostResponse> create(@Valid @RequestBody CreatePostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(communityService.createPost(request));
    }

    @GetMapping
    public List<PostResponse> list() {
        return communityService.listPosts();
    }

    /** Listado completo (incluye FLAGGED/HIDDEN) para administración. */
    @GetMapping("/all")
    public List<PostResponse> listAll() {
        return communityService.listAllPosts();
    }

    @GetMapping("/{id}")
    public PostDetailResponse get(@PathVariable UUID id) {
        return communityService.getPost(id);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable UUID id,
                                                      @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(communityService.addComment(id, request));
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> listComments(@PathVariable UUID id) {
        return communityService.listComments(id);
    }

    @PostMapping("/{id}/like")
    public LikeResponse like(@PathVariable UUID id, @Valid @RequestBody LikeRequest request) {
        return communityService.like(id, request.userId());
    }

    @DeleteMapping("/{id}/like")
    public LikeResponse unlike(@PathVariable UUID id, @RequestParam UUID userId) {
        return communityService.unlike(id, userId);
    }

    @PostMapping("/{id}/moderate")
    public PostResponse moderate(@PathVariable UUID id, @Valid @RequestBody ModerationRequest request) {
        return communityService.moderate(id, request.status());
    }
}
