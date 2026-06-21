package com.membership.auth.service;

import com.membership.auth.domain.Role;
import com.membership.auth.domain.User;
import com.membership.auth.dto.AuthDtos.AuthResponse;
import com.membership.auth.dto.AuthDtos.LoginRequest;
import com.membership.auth.dto.AuthDtos.RegisterRequest;
import com.membership.auth.repository.RoleRepository;
import com.membership.auth.repository.UserRepository;
import com.membership.live.security.JwtService;
import com.membership.service.MembershipService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "ROLE_MEMBER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final MembershipService membershipService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       JwtService jwtService, MembershipService membershipService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtService = jwtService;
        this.membershipService = membershipService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        Role memberRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException(DEFAULT_ROLE + " not seeded"));

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setFullName(req.fullName());
        user.setEnabled(true);
        user.getRoles().add(memberRole);

        userRepository.save(user);
        // Provisiona una membresía ACTIVE por defecto para que el nuevo miembro pueda usar /live.
        membershipService.provisionDefault(user.getId());
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User disabled");
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        // Reutiliza el JwtService del módulo de directos: el token vale también para el WebSocket.
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        String token = jwtService.generate(user.getId().toString(), roles);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), roles);
    }
}
