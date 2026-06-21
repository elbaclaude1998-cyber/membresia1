package com.membership.dto;

import com.membership.domain.MembershipStatus;
import com.membership.domain.RenewalType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class MembershipDtos {

    private MembershipDtos() { }

    /** Respuesta de /membership/status. */
    public record MembershipStatusResponse(
            UUID membershipId,
            MembershipStatus status,
            Instant endDate,
            boolean autoRenew,
            long daysRemaining) { }

    /** Vista completa de la membresía (renovación / cancelación). */
    public record MembershipResponse(
            UUID id,
            UUID userId,
            String plan,
            MembershipStatus status,
            BigDecimal price,
            String currency,
            boolean autoRenew,
            Instant startDate,
            Instant endDate,
            Instant createdAt,
            Instant updatedAt) { }

    public record PaymentResponse(
            UUID id,
            BigDecimal amount,
            String currency,
            int months,
            RenewalType renewalType,
            Instant paidAt) { }

    /** Body de /membership/renew (renovación manual). */
    public record RenewRequest(
            @NotNull UUID membershipId,
            @Min(1) int months,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
            @Size(min = 3, max = 3) String currency) { }

    /** Body de /membership/cancel. */
    public record CancelRequest(
            @NotNull UUID membershipId) { }
}
