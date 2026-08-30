package dev.phlawless.agentengine.account.domain;

import java.util.UUID;

public record AccountIdentity(UUID accountId, String username) {
}
