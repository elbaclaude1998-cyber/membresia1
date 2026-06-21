package com.membership.admin.web;

import com.membership.admin.dto.AdminDtos.AdminMembershipResponse;
import com.membership.admin.dto.AdminDtos.AdminUserResponse;
import com.membership.admin.dto.AdminDtos.StatsResponse;
import com.membership.admin.service.AdminService;
import com.membership.content.dto.ContentDtos.ModuleDetailResponse;
import com.membership.content.service.ContentService;
import com.membership.reservation.dto.ReservationDtos.ReservationResponse;
import com.membership.reservation.service.ReservationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final ContentService contentService;
    private final ReservationService reservationService;

    public AdminController(AdminService adminService, ContentService contentService,
                          ReservationService reservationService) {
        this.adminService = adminService;
        this.contentService = contentService;
        this.reservationService = reservationService;
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users() {
        return adminService.users();
    }

    @GetMapping("/memberships")
    public List<AdminMembershipResponse> memberships() {
        return adminService.memberships();
    }

    @GetMapping("/content")
    public List<ModuleDetailResponse> content() {
        return contentService.adminListModules();
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return adminService.stats();
    }

    @GetMapping("/reservations")
    public List<ReservationResponse> reservations() {
        return reservationService.listAll();
    }
}
