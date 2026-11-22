package com.solvestack.security;

import com.solvestack.entity.TrackerKind;
import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;
import java.util.UUID;

public record TrackerPrincipal(UUID ownerId, TrackerKind kind, String email) implements Principal, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return ownerId.toString();
    }
}
