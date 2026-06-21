package com.membership.reservation.web;

import com.membership.reservation.dto.ReservationDtos.CreateReservationRequest;
import com.membership.reservation.dto.ReservationDtos.CreateReservationResponse;
import com.membership.reservation.dto.ReservationDtos.ReservationResponse;
import com.membership.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<CreateReservationResponse> create(@Valid @RequestBody CreateReservationRequest request,
                                                            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(currentUser(auth), request));
    }

    @GetMapping("/mine")
    public List<ReservationResponse> mine(Authentication auth) {
        return reservationService.mine(currentUser(auth));
    }

    @PostMapping("/{id}/confirm")
    public ReservationResponse confirm(@PathVariable UUID id, Authentication auth) {
        return reservationService.confirm(id, currentUser(auth));
    }

    private UUID currentUser(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}
