package com.membership.security;

import com.membership.domain.MembershipStatus;
import com.membership.repository.MembershipRepository;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Autoriza el acceso a /live/** solo a usuarios con una membresía ACTIVE.
 */
@Component
public class ActiveMemberAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final MembershipRepository membershipRepository;

    public ActiveMemberAuthorizationManager(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return new AuthorizationDecision(false);
        }
        try {
            UUID userId = UUID.fromString(auth.getName());
            boolean active = membershipRepository.existsByUserIdAndStatus(userId, MembershipStatus.ACTIVE);
            return new AuthorizationDecision(active);
        } catch (IllegalArgumentException e) {
            return new AuthorizationDecision(false);
        }
    }
}
