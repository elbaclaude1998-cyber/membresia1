package com.membership.content.web;

import com.membership.content.dto.ContentDtos.ItemResponse;
import com.membership.content.dto.ContentDtos.ModuleDetailResponse;
import com.membership.content.dto.ContentDtos.ModuleResponse;
import com.membership.content.dto.ContentDtos.ProgressRequest;
import com.membership.content.service.ContentService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/content")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/modules")
    public List<ModuleResponse> modules() {
        return contentService.listModules();
    }

    @GetMapping("/modules/{id}")
    public ModuleDetailResponse module(@PathVariable UUID id, Authentication auth) {
        return contentService.getModule(id, currentUser(auth));
    }

    @GetMapping("/items/{id}")
    public ItemResponse item(@PathVariable UUID id, Authentication auth) {
        return contentService.getItem(id, currentUser(auth));
    }

    @PostMapping("/items/{id}/progress")
    public ItemResponse progress(@PathVariable UUID id, @Valid @RequestBody ProgressRequest request, Authentication auth) {
        return contentService.setProgress(id, currentUser(auth), request.progress());
    }

    private UUID currentUser(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}
