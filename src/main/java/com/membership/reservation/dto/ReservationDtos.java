package com.membership.reservation.dto;

import com.membership.reservation.domain.ReservationStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class ReservationDtos {

    private ReservationDtos() { }

    public record CreateReservationRequest(
            @NotNull Instant fechaHora,
            @Min(1) int duracionMinutos,
            String paymentMethod) { }

    public record ReservationResponse(
            UUID id,
            UUID userId,
            Instant fechaHora,
            int duracionMinutos,
            ReservationStatus estado,
            int amountCents,
            Instant createdAt) { }

    /** Datos para que el front construya el formulario y haga POST al TPV. */
    public record RedsysForm(
            String url,
            String signatureVersion,
            String merchantParameters,
            String signature,
            String paymentMethod) { }

    public record CreateReservationResponse(
            ReservationResponse reservation,
            RedsysForm redsys) { }
}
