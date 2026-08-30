package dev.phlawless.agentengine.game.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record GameEvent(
        long sequence,
        int turn,
        String type,
        Instant occurredAt,
        UUID actorAccountId,
        Integer actorSeat,
        Map<String, String> details
) {
}
