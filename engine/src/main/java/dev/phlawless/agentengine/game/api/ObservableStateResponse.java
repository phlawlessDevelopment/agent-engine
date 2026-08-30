package dev.phlawless.agentengine.game.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ObservableStateResponse(
        UUID gameId,
        String gameType,
        List<String> actions,
        int turn,
        Map<String, Object> state,
        Instant createdAt,
        Instant updatedAt
) {
}
