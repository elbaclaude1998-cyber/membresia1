package com.membership.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ActiveMemberAuthorizationManager activeMember;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          ActiveMemberAuthorizationManager activeMember) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.activeMember = activeMember;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Públicos
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()                               // handshake; el STOMP CONNECT valida JWT
                        .requestMatchers(HttpMethod.GET, "/live/dev-token").permitAll()      // solo desarrollo
                        .requestMatchers("/redsys/**").permitAll()                           // callback servidor-a-servidor de Redsys
                        // Solo ADMIN
                        .requestMatchers("/posts/all").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/posts/*/moderate").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Autenticados
                        .requestMatchers("/membership/**").authenticated()
                        .requestMatchers("/content/**").authenticated()
                        .requestMatchers("/reservations/**").authenticated()
                        // Solo miembros activos
                        .requestMatchers("/live/**").access(activeMember)
                        // Resto (posts, comments, likes, notifications) autenticado
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
