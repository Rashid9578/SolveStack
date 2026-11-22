package com.solvestack.security;

import com.solvestack.config.SessionCookieConfig;
import com.solvestack.entity.TrackerKind;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
public class SessionAuthentication {
    private static final Duration ACCOUNT_TIMEOUT = Duration.ofMinutes(30);

    private final SecurityContextRepository contexts;
    private final SecurityContextLogoutHandler logout = new SecurityContextLogoutHandler();

    public SessionAuthentication(SecurityContextRepository contexts) {
        this.contexts = contexts;
    }

    public void start(TrackerPrincipal principal, HttpServletRequest request, HttpServletResponse response) {
        clear(request, response);

        HttpSession session = request.getSession(true);
        boolean guest = principal.kind() == TrackerKind.GUEST;
        Duration timeout = guest ? SessionCookieConfig.GUEST_LIFETIME : ACCOUNT_TIMEOUT;
        session.setMaxInactiveInterval(Math.toIntExact(timeout.toSeconds()));
        if (guest) {
            request.setAttribute(SessionCookieConfig.PERSIST_GUEST_COOKIE, true);
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        var authority = new SimpleGrantedAuthority("ROLE_" + principal.kind().name());
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(authority)));
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
    }

    public void clear(HttpServletRequest request, HttpServletResponse response) {
        logout.logout(request, response, null);
    }
}
