package dev.phlawless.agentengine.game.api;

import java.util.UUID;

public record GameParticipantResponse(UUID accountId, String username, int seat) {
}
