package com.membership.admin.service;

import com.membership.admin.dto.AdminDtos.AdminMembershipResponse;
import com.membership.admin.dto.AdminDtos.AdminUserResponse;
import com.membership.admin.dto.AdminDtos.StatsResponse;
import com.membership.auth.domain.Role;
import com.membership.auth.repository.UserRepository;
import com.membership.community.repository.PostRepository;
import com.membership.content.repository.ContentItemRepository;
import com.membership.content.repository.ContentModuleRepository;
import com.membership.domain.MembershipStatus;
import com.membership.live.repository.LiveEventRepository;
import com.membership.repository.MembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PostRepository postRepository;
    private final LiveEventRepository liveEventRepository;
    private final ContentModuleRepository contentModuleRepository;
    private final ContentItemRepository contentItemRepository;

    public AdminService(UserRepository userRepository,
                        MembershipRepository membershipRepository,
                        PostRepository postRepository,
                        LiveEventRepository liveEventRepository,
                        ContentModuleRepository contentModuleRepository,
                        ContentItemRepository contentItemRepository) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.postRepository = postRepository;
        this.liveEventRepository = liveEventRepository;
        this.contentModuleRepository = contentModuleRepository;
        this.contentItemRepository = contentItemRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> users() {
        return userRepository.findAll().stream()
                .map(u -> new AdminUserResponse(
                        u.getId(), u.getEmail(), u.getFullName(), u.isEnabled(),
                        u.getRoles().stream().map(Role::getName).toList(),
                        u.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminMembershipResponse> memberships() {
        return membershipRepository.findAll().stream()
                .map(m -> new AdminMembershipResponse(
                        m.getId(), m.getUserId(), m.getPlan(), m.getStatus().name(),
                        m.isAutoRenew(), m.getEndDate()))
                .toList();
    }

    @Transactional(readOnly = true)
    public StatsResponse stats() {
        return new StatsResponse(
                userRepository.count(),
                membershipRepository.countByStatus(MembershipStatus.ACTIVE),
                postRepository.count(),
                liveEventRepository.count(),
                contentModuleRepository.count(),
                contentItemRepository.count());
    }
}
