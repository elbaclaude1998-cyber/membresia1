package com.membership.web;

import com.membership.dto.MembershipDtos.CancelRequest;
import com.membership.dto.MembershipDtos.MembershipResponse;
import com.membership.dto.MembershipDtos.MembershipStatusResponse;
import com.membership.dto.MembershipDtos.RenewRequest;
import com.membership.service.MembershipService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/membership")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping("/status/{membershipId}")
    public MembershipStatusResponse status(@PathVariable UUID membershipId) {
        return membershipService.status(membershipId);
    }

    /** Membresía del usuario autenticado. */
    @GetMapping("/me")
    public MembershipResponse me(Authentication auth) {
        return membershipService.getByUser(UUID.fromString(auth.getName()));
    }

    /** Membresía de un usuario concreto. */
    @GetMapping("/by-user/{userId}")
    public MembershipResponse byUser(@PathVariable UUID userId) {
        return membershipService.getByUser(userId);
    }

    @PostMapping("/renew")
    public MembershipResponse renew(@Valid @RequestBody RenewRequest request) {
        return membershipService.renew(request);
    }

    @PostMapping("/cancel")
    public MembershipResponse cancel(@Valid @RequestBody CancelRequest request) {
        return membershipService.cancel(request.membershipId());
    }
}
