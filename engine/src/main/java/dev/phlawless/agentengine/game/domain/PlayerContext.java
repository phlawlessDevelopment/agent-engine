package dev.phlawless.agentengine.game.domain;

import java.util.UUID;

public record PlayerContext(UUID accountId, int seat) {
}
