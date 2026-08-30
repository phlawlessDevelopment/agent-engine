package dev.phlawless.agentengine.game.domain;

import java.util.UUID;

public record GameParticipant(UUID accountId, String username, int seat) {
}
