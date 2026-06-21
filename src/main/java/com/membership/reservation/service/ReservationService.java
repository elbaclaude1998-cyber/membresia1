package com.membership.reservation.service;

import com.membership.reservation.domain.Reservation;
import com.membership.reservation.domain.ReservationStatus;
import com.membership.reservation.dto.ReservationDtos.CreateReservationRequest;
import com.membership.reservation.dto.ReservationDtos.CreateReservationResponse;
import com.membership.reservation.dto.ReservationDtos.RedsysForm;
import com.membership.reservation.dto.ReservationDtos.ReservationResponse;
import com.membership.reservation.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ReservationService {

    private final ReservationRepository repository;
    private final RedsysService redsysService;
    private final int pricePerMinuteCents;

    public ReservationService(ReservationRepository repository,
                              RedsysService redsysService,
                              @Value("${redsys.price-per-minute-cents:50}") int pricePerMinuteCents) {
        this.repository = repository;
        this.redsysService = redsysService;
        this.pricePerMinuteCents = pricePerMinuteCents;
    }

    /** Crea la reserva en PENDIENTE_PAGO y genera los datos de pago Redsys. */
    @Transactional
    public CreateReservationResponse create(UUID userId, CreateReservationRequest req) {
        int amountCents = Math.max(1, req.duracionMinutos() * pricePerMinuteCents);
        String order = generateOrder();

        Reservation r = new Reservation();
        r.setUserId(userId);
        r.setFechaHora(req.fechaHora());
        r.setDuracionMinutos(req.duracionMinutos());
        r.setEstado(ReservationStatus.PENDIENTE_PAGO);
        r.setPaymentOrder(order);
        r.setAmountCents(amountCents);
        repository.save(r);

        RedsysForm redsys = redsysService.buildPaymentForm(order, amountCents, req.paymentMethod());
        return new CreateReservationResponse(toResponse(r), redsys);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> mine(UUID userId) {
        return repository.findByUserIdOrderByFechaHoraDesc(userId).stream().map(this::toResponse).toList();
    }

    /** Todas las reservas (panel admin). */
    @Transactional(readOnly = true)
    public List<ReservationResponse> listAll() {
        return repository.findAllByOrderByFechaHoraDesc().stream().map(this::toResponse).toList();
    }

    /** Confirmación manual (propietario) — útil para pruebas locales sin callback público. */
    @Transactional
    public ReservationResponse confirm(UUID id, UUID userId) {
        Reservation r = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
        if (!r.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No es tu reserva");
        }
        r.setEstado(ReservationStatus.CONFIRMADA);
        return toResponse(r);
    }

    /** Confirmación desde el callback de Redsys (por número de pedido). */
    @Transactional
    public void confirmByOrder(String order, boolean authorized) {
        repository.findByPaymentOrder(order).ifPresent(r -> {
            if (r.getEstado() == ReservationStatus.PENDIENTE_PAGO) {
                r.setEstado(authorized ? ReservationStatus.CONFIRMADA : ReservationStatus.CANCELADA);
            }
        });
    }

    private String generateOrder() {
        long secs = (System.currentTimeMillis() / 1000) % 10_000_000_000L; // 10 dígitos
        int rnd = ThreadLocalRandom.current().nextInt(100);               // 2 dígitos
        return String.format("%010d%02d", secs, rnd);                     // 12 dígitos, prefijo numérico
    }

    private ReservationResponse toResponse(Reservation r) {
        return new ReservationResponse(
                r.getId(), r.getUserId(), r.getFechaHora(), r.getDuracionMinutos(),
                r.getEstado(), r.getAmountCents(), r.getCreatedAt());
    }
}
