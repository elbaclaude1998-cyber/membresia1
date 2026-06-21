package com.membership.community.web;

import com.membership.community.dto.CommunityDtos.NotificationResponse;
import com.membership.community.service.CommunityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final CommunityService communityService;

    public NotificationController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping
    public List<NotificationResponse> list(@RequestParam UUID recipientId) {
        return communityService.notifications(recipientId);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        communityService.markRead(id);
        return ResponseEntity.noContent().build();
    }
}
