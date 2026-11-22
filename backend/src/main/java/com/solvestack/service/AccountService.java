package com.solvestack.service;

import com.solvestack.dto.LoginRequest;
import com.solvestack.dto.RegistrationRequest;
import com.solvestack.entity.TrackerKind;
import com.solvestack.entity.TrackerOwner;
import com.solvestack.exception.AccountConflictException;
import com.solvestack.repository.TrackerOwnerRepository;
import com.solvestack.security.TrackerPrincipal;
import java.util.Locale;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private static final String EMAIL_CONSTRAINT = "tracker_owners_normalized_email_key";

    private final TrackerOwnerRepository owners;
    private final PasswordEncoder passwordEncoder;
    private final String missingAccountHash;

    public AccountService(TrackerOwnerRepository owners, PasswordEncoder passwordEncoder) {
        this.owners = owners;
        this.passwordEncoder = passwordEncoder;
        this.missingAccountHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public TrackerPrincipal createGuest() {
        return principal(owners.save(new TrackerOwner()));
    }

    @Transactional
    public TrackerPrincipal register(TrackerPrincipal current, RegistrationRequest request) {
        if (current != null && current.kind() == TrackerKind.ACCOUNT) {
            throw new AccountConflictException("Sign out before creating another account.");
        }

        String email = normalize(request.email());
        String hash = passwordEncoder.encode(request.password());
        TrackerOwner owner = current == null ? new TrackerOwner() : guestForUpgrade(current.ownerId());
        owner.register(email, hash);

        try {
            return principal(owners.saveAndFlush(owner));
        } catch (DataIntegrityViolationException error) {
            if (error.getCause() instanceof ConstraintViolationException violation
                    && EMAIL_CONSTRAINT.equals(violation.getConstraintName())) {
                throw new AccountConflictException("An account already exists for this email.");
            }
            throw error;
        }
    }

    public TrackerPrincipal login(LoginRequest request) {
        TrackerOwner owner = owners.findByNormalizedEmail(normalize(request.email())).orElse(null);
        String hash = owner == null ? missingAccountHash : owner.getPasswordHash();
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (!matches || owner == null) {
            throw new BadCredentialsException("The email or password is incorrect.");
        }
        return principal(owner);
    }

    private TrackerOwner guestForUpgrade(UUID ownerId) {
        return owners.findForUpgrade(ownerId)
                .filter(owner -> owner.getKind() == TrackerKind.GUEST)
                .orElseThrow(() -> new AccountConflictException("Your guest session changed. Reload the page and try again."));
    }

    private static String normalize(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    private static TrackerPrincipal principal(TrackerOwner owner) {
        return new TrackerPrincipal(owner.getId(), owner.getKind(), owner.getNormalizedEmail());
    }
}
