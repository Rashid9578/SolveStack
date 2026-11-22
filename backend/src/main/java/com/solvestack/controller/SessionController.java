package com.solvestack.controller;

import com.solvestack.dto.LoginRequest;
import com.solvestack.dto.RegistrationRequest;
import com.solvestack.dto.SessionResponse;
import com.solvestack.security.SessionAuthentication;
import com.solvestack.security.TrackerPrincipal;
import com.solvestack.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
public class SessionController {
    private final AccountService accounts;
    private final SessionAuthentication sessions;

    public SessionController(AccountService accounts, SessionAuthentication sessions) {
        this.accounts = accounts;
        this.sessions = sessions;
    }

    @GetMapping
    public ResponseEntity<SessionResponse> current(@AuthenticationPrincipal TrackerPrincipal principal) {
        return sessionResponse(HttpStatus.OK, principal);
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfToken> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(token);
    }

    @PostMapping("/guest")
    public ResponseEntity<SessionResponse> guest(@AuthenticationPrincipal TrackerPrincipal principal,
            HttpServletRequest request, HttpServletResponse response) {
        if (principal != null) {
            return sessionResponse(HttpStatus.OK, principal);
        }
        TrackerPrincipal guest = accounts.createGuest();
        sessions.start(guest, request, response);
        return sessionResponse(HttpStatus.CREATED, guest);
    }

    @PostMapping("/register")
    public ResponseEntity<SessionResponse> register(@AuthenticationPrincipal TrackerPrincipal principal,
            @Valid @RequestBody RegistrationRequest registration,
            HttpServletRequest request, HttpServletResponse response) {
        TrackerPrincipal account = accounts.register(principal, registration);
        sessions.start(account, request, response);
        return sessionResponse(HttpStatus.CREATED, account);
    }

    @PostMapping("/login")
    public ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest login,
            HttpServletRequest request, HttpServletResponse response) {
        TrackerPrincipal account = accounts.login(login);
        sessions.start(account, request, response);
        return sessionResponse(HttpStatus.OK, account);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        sessions.clear(request, response);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    private static ResponseEntity<SessionResponse> sessionResponse(HttpStatus status, TrackerPrincipal principal) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(SessionResponse.from(principal));
    }
}
