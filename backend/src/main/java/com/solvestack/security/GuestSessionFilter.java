package com.solvestack.security;

import com.solvestack.entity.TrackerKind;
import com.solvestack.repository.TrackerOwnerRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

public class GuestSessionFilter extends OncePerRequestFilter {
    private final TrackerOwnerRepository owners;
    private final SecurityContextLogoutHandler logout = new SecurityContextLogoutHandler();

    public GuestSessionFilter(TrackerOwnerRepository owners) {
        this.owners = owners;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/readyz")
            || path.equals("/livez")
            || path.equals("/api/topics")
            || path.startsWith("/api/topics/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof TrackerPrincipal principal
                && principal.kind() == TrackerKind.GUEST
                && !owners.existsByIdAndKind(principal.ownerId(), TrackerKind.GUEST)) {
            logout.logout(request, response, authentication);
        }
        chain.doFilter(request, response);
    }
}
