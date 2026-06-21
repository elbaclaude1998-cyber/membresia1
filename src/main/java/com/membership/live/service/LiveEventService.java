package com.membership.live.service;

import com.membership.live.domain.EventStatus;
import com.membership.live.domain.LiveEvent;
import com.membership.live.dto.LiveDtos.CreateLiveEventRequest;
import com.membership.live.dto.LiveDtos.LiveEventResponse;
import com.membership.live.repository.LiveEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class LiveEventService {

    private final LiveEventRepository repository;

    public LiveEventService(LiveEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public LiveEventResponse create(CreateLiveEventRequest req) {
        LiveEvent event = new LiveEvent();
        event.setTitle(req.title());
        event.setDescription(req.description());
        event.setStartsAt(req.startsAt());
        event.setHostId(req.hostId());
        event.setStatus(EventStatus.SCHEDULED);
        repository.save(event);
        return toResponse(event);
    }

    @Transactional(readOnly = true)
    public List<LiveEventResponse> list() {
        return repository.findAllByOrderByStartsAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LiveEventResponse get(UUID id) {
        return toResponse(load(id));
    }

    /** Directos en curso (estado LIVE). */
    @Transactional(readOnly = true)
    public List<LiveEventResponse> now() {
        return repository.findByStatusOrderByStartsAtAsc(EventStatus.LIVE).stream().map(this::toResponse).toList();
    }

    @Transactional
    public LiveEventResponse start(UUID id) {
        LiveEvent event = load(id);
        if (event.getStatus() == EventStatus.ENDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event already ended");
        }
        event.setStatus(EventStatus.LIVE);
        return toResponse(event);
    }

    @Transactional
    public LiveEventResponse end(UUID id) {
        LiveEvent event = load(id);
        event.setStatus(EventStatus.ENDED);
        return toResponse(event);
    }

    private LiveEvent load(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Live event not found"));
    }

    private LiveEventResponse toResponse(LiveEvent e) {
        return new LiveEventResponse(
                e.getId(), e.getTitle(), e.getDescription(), e.getStartsAt(),
                e.getStatus(), e.getHostId(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
