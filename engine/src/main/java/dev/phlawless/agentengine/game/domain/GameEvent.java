package dev.phlawless.agentengine.game.domain;

import java.time.Instant;
import java.util.Map;

public record GameEvent(
        long sequence,
        int turn,
        String type,
        Instant occurredAt,
        Map<String, String> details
) {
}
