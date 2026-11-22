package com.solvestack.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.solvestack.security.TrackerPrincipal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionResponse(String kind, String email) {
    public static SessionResponse from(TrackerPrincipal principal) {
        return principal == null
                ? new SessionResponse("ANONYMOUS", null)
                : new SessionResponse(principal.kind().name(), principal.email());
    }
}
