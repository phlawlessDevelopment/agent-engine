package dev.phlawless.agentengine.security.api;

import java.util.UUID;

public record AuthenticatedAccountResponse(UUID accountId, String username) {
}
