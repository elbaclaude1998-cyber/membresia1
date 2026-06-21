package com.membership.content.service;

import com.membership.content.domain.ContentItem;
import com.membership.content.domain.ContentModule;
import com.membership.content.domain.ContentProgress;
import com.membership.content.dto.ContentDtos.ItemResponse;
import com.membership.content.dto.ContentDtos.ModuleDetailResponse;
import com.membership.content.dto.ContentDtos.ModuleResponse;
import com.membership.content.repository.ContentItemRepository;
import com.membership.content.repository.ContentModuleRepository;
import com.membership.content.repository.ContentProgressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ContentService {

    private final ContentModuleRepository moduleRepository;
    private final ContentItemRepository itemRepository;
    private final ContentProgressRepository progressRepository;

    public ContentService(ContentModuleRepository moduleRepository,
                          ContentItemRepository itemRepository,
                          ContentProgressRepository progressRepository) {
        this.moduleRepository = moduleRepository;
        this.itemRepository = itemRepository;
        this.progressRepository = progressRepository;
    }

    @Transactional(readOnly = true)
    public List<ModuleResponse> listModules() {
        return moduleRepository.findAllByOrderByPositionAsc().stream()
                .map(m -> toModuleResponse(m, itemRepository.countByModuleId(m.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ModuleDetailResponse getModule(UUID moduleId, UUID userId) {
        ContentModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Module not found"));
        List<ContentItem> items = itemRepository.findByModuleIdOrderByPositionAsc(moduleId);
        Map<UUID, ContentProgress> progress = progressByItem(userId);
        List<ItemResponse> itemResponses = items.stream()
                .map(i -> toItemResponse(i, progress.get(i.getId())))
                .toList();
        return new ModuleDetailResponse(toModuleResponse(module, items.size()), itemResponses);
    }

    @Transactional(readOnly = true)
    public ItemResponse getItem(UUID itemId, UUID userId) {
        ContentItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        ContentProgress p = progressRepository.findByUserIdAndItemId(userId, itemId).orElse(null);
        return toItemResponse(item, p);
    }

    @Transactional
    public ItemResponse setProgress(UUID itemId, UUID userId, int progressValue) {
        ContentItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        ContentProgress p = progressRepository.findByUserIdAndItemId(userId, itemId)
                .orElseGet(() -> {
                    ContentProgress np = new ContentProgress();
                    np.setUserId(userId);
                    np.setItem(item);
                    return np;
                });
        p.setProgress(progressValue);
        p.setCompleted(progressValue >= 100);
        progressRepository.save(p);
        return toItemResponse(item, p);
    }

    /** Vista de administración: todos los módulos con sus ítems (sin progreso de usuario). */
    @Transactional(readOnly = true)
    public List<ModuleDetailResponse> adminListModules() {
        return moduleRepository.findAllByOrderByPositionAsc().stream()
                .map(m -> {
                    List<ContentItem> items = itemRepository.findByModuleIdOrderByPositionAsc(m.getId());
                    List<ItemResponse> itemResponses = items.stream().map(i -> toItemResponse(i, null)).toList();
                    return new ModuleDetailResponse(toModuleResponse(m, items.size()), itemResponses);
                })
                .toList();
    }

    private Map<UUID, ContentProgress> progressByItem(UUID userId) {
        return progressRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(p -> p.getItem().getId(), Function.identity()));
    }

    private ModuleResponse toModuleResponse(ContentModule m, long itemCount) {
        return new ModuleResponse(m.getId(), m.getTitle(), m.getDescription(), m.getPosition(), itemCount, m.getCreatedAt());
    }

    private ItemResponse toItemResponse(ContentItem i, ContentProgress p) {
        return new ItemResponse(
                i.getId(), i.getModule().getId(), i.getTitle(), i.getType(), i.getUrl(),
                i.getDurationSeconds(), i.getPosition(),
                p != null ? p.getProgress() : 0,
                p != null && p.isCompleted());
    }
}
