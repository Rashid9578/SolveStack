package com.solvestack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
public record CompletionId(
        @Column(name = "owner_id") UUID ownerId,
        @Column(name = "problem_id") String problemId) implements Serializable {}
