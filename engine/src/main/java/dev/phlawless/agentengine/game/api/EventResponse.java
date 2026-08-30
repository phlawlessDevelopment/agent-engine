package dev.phlawless.agentengine.game.api;

import java.time.Instant;
import java.util.Map;

public record EventResponse(
        long sequence,
        int turn,
        String type,
        Instant occurredAt,
        Map<String, String> details
) {
}
