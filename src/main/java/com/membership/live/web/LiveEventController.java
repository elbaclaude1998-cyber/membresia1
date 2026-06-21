package com.membership.live.web;

import com.membership.live.dto.LiveDtos.CreateLiveEventRequest;
import com.membership.live.dto.LiveDtos.LiveEventResponse;
import com.membership.live.service.LiveEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/live")
public class LiveEventController {

    private final LiveEventService service;

    public LiveEventController(LiveEventService service) {
        this.service = service;
    }

    @PostMapping("/events")
    public ResponseEntity<LiveEventResponse> create(@Valid @RequestBody CreateLiveEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/events")
    public List<LiveEventResponse> list() {
        return service.list();
    }

    @GetMapping("/events/{id}")
    public LiveEventResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/now")
    public List<LiveEventResponse> now() {
        return service.now();
    }

    @PostMapping("/events/{id}/start")
    public LiveEventResponse start(@PathVariable UUID id) {
        return service.start(id);
    }

    @PostMapping("/events/{id}/end")
    public LiveEventResponse end(@PathVariable UUID id) {
        return service.end(id);
    }
}
