package dev.phlawless.agentengine.account.domain;

import java.time.Instant;
import java.util.UUID;

public record Account(
        UUID id,
        String username,
        String passwordHash,
        Instant createdAt
) {
}
