package com.solvestack.repository;

import com.solvestack.entity.TrackerKind;
import com.solvestack.entity.TrackerOwner;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TrackerOwnerRepository extends JpaRepository<TrackerOwner, UUID> {
    Optional<TrackerOwner> findByNormalizedEmail(String normalizedEmail);

    boolean existsByIdAndKind(UUID id, TrackerKind kind);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select owner from TrackerOwner owner where owner.id = :id")
    Optional<TrackerOwner> findForUpgrade(UUID id);
}
