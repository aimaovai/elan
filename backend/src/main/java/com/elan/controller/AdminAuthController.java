package com.elan.controller;

import com.elan.dto.AdminAuthDtos.*;
import com.elan.entity.Administrator;
import com.elan.repository.AdministratorRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final AdministratorRepository administrators;

    public AdminAuthController(AuthenticationManager authenticationManager,
                               SecurityContextRepository contextRepository,
                               AdministratorRepository administrators) {
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
        this.administrators = administrators;
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfView> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new CsrfView(token.getToken(), token.getHeaderName()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest servletRequest, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.email().trim().toLowerCase(Locale.ROOT), request.password()));
            servletRequest.getSession(true);
            servletRequest.changeSessionId();
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            contextRepository.saveContext(context, servletRequest, response);
            Administrator administrator = administrators.findByEmailIgnoreCase(authentication.getName())
                    .filter(value -> value.active)
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
            administrator.lastLoginAt = LocalDateTime.now();
            administrator.updatedAt = administrator.lastLoginAt;
            administrators.save(administrator);
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(new AdministratorView(administrator.email, administrator.displayName, administrator.role));
        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(CacheControl.noStore())
                    .body(java.util.Map.of("code", "AUTHENTICATION_FAILED",
                            "message", "Email or password is incorrect."));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<AdministratorView> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(CacheControl.noStore()).build();
        return administrators.findByEmailIgnoreCase(authentication.getName())
                .filter(value -> value.active)
                .map(value -> ResponseEntity.ok().cacheControl(CacheControl.noStore())
                        .body(new AdministratorView(value.email, value.displayName, value.role)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(CacheControl.noStore()).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
