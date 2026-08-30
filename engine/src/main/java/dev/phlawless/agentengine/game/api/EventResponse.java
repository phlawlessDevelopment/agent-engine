package dev.phlawless.agentengine.game.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EventResponse(
        long sequence,
        int turn,
        String type,
        Instant occurredAt,
        UUID actorAccountId,
        Integer actorSeat,
        Map<String, String> details
) {
}
