package com.membership.community.mapper;

import com.membership.community.domain.Comment;
import com.membership.community.domain.Notification;
import com.membership.community.domain.Post;
import com.membership.community.dto.CommunityDtos.CommentResponse;
import com.membership.community.dto.CommunityDtos.NotificationResponse;
import com.membership.community.dto.CommunityDtos.PostResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CommunityMapper {

    @Mapping(target = "commentCount", source = "commentCount")
    PostResponse toResponse(Post post, long commentCount);

    @Mapping(target = "postId", source = "post.id")
    CommentResponse toResponse(Comment comment);

    List<CommentResponse> toCommentResponses(List<Comment> comments);

    NotificationResponse toResponse(Notification notification);
}
