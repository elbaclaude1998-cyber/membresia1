package com.membership.reservation.repository;

import com.membership.reservation.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    List<Reservation> findByUserIdOrderByFechaHoraDesc(UUID userId);
    List<Reservation> findAllByOrderByFechaHoraDesc();
    Optional<Reservation> findByPaymentOrder(String paymentOrder);
}
