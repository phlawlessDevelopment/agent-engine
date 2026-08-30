package dev.phlawless.agentengine.account.api;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(UUID accountId, String username, Instant createdAt) {
}
