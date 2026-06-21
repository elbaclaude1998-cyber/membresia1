package com.membership.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AutoRenewalScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutoRenewalScheduler.class);

    private final MembershipService membershipService;

    public AutoRenewalScheduler(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    /** Cada hora en punto: renueva las membresías vencidas con auto-renovación activa. */
    @Scheduled(cron = "${membership.auto-renewal.cron:0 0 * * * *}")
    public void runAutoRenewals() {
        int renewed = membershipService.processAutoRenewals();
        if (renewed > 0) {
            log.info("Auto-renewal completed: {} memberships renewed", renewed);
        }
    }
}
