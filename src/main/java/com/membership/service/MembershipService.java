package com.membership.service;

import com.membership.domain.Membership;
import com.membership.domain.MembershipStatus;
import com.membership.domain.Payment;
import com.membership.domain.RenewalType;
import com.membership.dto.MembershipDtos.MembershipResponse;
import com.membership.dto.MembershipDtos.MembershipStatusResponse;
import com.membership.dto.MembershipDtos.RenewRequest;
import com.membership.mapper.MembershipMapper;
import com.membership.repository.MembershipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final MembershipMapper mapper;

    public MembershipService(MembershipRepository membershipRepository, MembershipMapper mapper) {
        this.membershipRepository = membershipRepository;
        this.mapper = mapper;
    }

    /** GET /membership/status — recalcula EXPIRED de forma perezosa. */
    @Transactional
    public MembershipStatusResponse status(UUID membershipId) {
        Membership m = load(membershipId);
        MembershipStatus evaluated = evaluate(m);
        if (evaluated != m.getStatus()) {
            m.setStatus(evaluated);
        }
        long daysRemaining = daysRemaining(m);
        return new MembershipStatusResponse(
                m.getId(), m.getStatus(), m.getEndDate(), m.isAutoRenew(), daysRemaining);
    }

    /** POST /membership/renew — renovación manual. */
    @Transactional
    public MembershipResponse renew(RenewRequest req) {
        Membership m = load(req.membershipId());
        if (m.getStatus() == MembershipStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot renew a cancelled membership");
        }
        applyRenewal(m, req.months(), req.amount(), req.currency(), RenewalType.MANUAL);
        return mapper.toResponse(m);
    }

    /** POST /membership/cancel. */
    @Transactional
    public MembershipResponse cancel(UUID membershipId) {
        Membership m = load(membershipId);
        if (m.getStatus() == MembershipStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Membership already cancelled");
        }
        m.setStatus(MembershipStatus.CANCELLED);
        m.setAutoRenew(false);
        return mapper.toResponse(m);
    }

    /** Membresía del usuario indicado (para /membership/me y /membership/by-user). */
    @Transactional
    public MembershipResponse getByUser(UUID userId) {
        Membership m = membershipRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found"));
        MembershipStatus evaluated = evaluate(m);
        if (evaluated != m.getStatus()) {
            m.setStatus(evaluated);
        }
        return mapper.toResponse(m);
    }

    /**
     * Provisiona una membresía ACTIVE por defecto para un usuario que no tenga ninguna.
     * Se invoca al registrarse, de modo que cualquier miembro puede acceder a /live.
     */
    @Transactional
    public void provisionDefault(UUID userId) {
        if (membershipRepository.findByUserId(userId).isPresent()) {
            return;
        }
        Instant now = Instant.now();
        Membership m = new Membership();
        m.setUserId(userId);
        m.setPlan("FREE");
        m.setPrice(BigDecimal.ZERO);
        m.setCurrency("EUR");
        m.setRenewalMonths(1);
        m.setAutoRenew(false);
        m.setStatus(MembershipStatus.ACTIVE);
        m.setStartDate(now);
        m.setEndDate(now.atZone(ZoneOffset.UTC).plusMonths(1).toInstant());
        membershipRepository.save(m);
    }

    /** Renovación automática (invocada por el scheduler). */
    @Transactional
    public int processAutoRenewals() {
        List<Membership> due = membershipRepository.findDueForAutoRenewal(Instant.now());
        for (Membership m : due) {
            applyRenewal(m, m.getRenewalMonths(), m.getPrice(), m.getCurrency(), RenewalType.AUTO);
        }
        return due.size();
    }

    // ---- internos ----

    private Payment applyRenewal(Membership m, int months, BigDecimal amount, String currency, RenewalType type) {
        Instant now = Instant.now();
        // Si aún está vigente, se acumula sobre el endDate; si caducó, parte de ahora.
        Instant base = (m.getEndDate() != null && m.getEndDate().isAfter(now)) ? m.getEndDate() : now;
        Instant newEnd = base.atZone(ZoneOffset.UTC).plusMonths(months).toInstant();

        Payment payment = new Payment();
        payment.setMembership(m);
        payment.setAmount(amount);
        payment.setCurrency(currency != null && !currency.isBlank() ? currency : m.getCurrency());
        payment.setMonths(months);
        payment.setRenewalType(type);
        payment.setPaidAt(now);

        m.getPayments().add(payment);
        m.setEndDate(newEnd);
        m.setStatus(MembershipStatus.ACTIVE);
        return payment;
    }

    private MembershipStatus evaluate(Membership m) {
        if (m.getStatus() == MembershipStatus.CANCELLED) {
            return MembershipStatus.CANCELLED;
        }
        if (m.getEndDate() != null && m.getEndDate().isBefore(Instant.now())) {
            return MembershipStatus.EXPIRED;
        }
        return MembershipStatus.ACTIVE;
    }

    private long daysRemaining(Membership m) {
        Instant now = Instant.now();
        if (m.getEndDate() == null || !m.getEndDate().isAfter(now)) {
            return 0;
        }
        return Duration.between(now, m.getEndDate()).toDays();
    }

    private Membership load(UUID membershipId) {
        return membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found"));
    }
}
