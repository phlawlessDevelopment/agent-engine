package dev.phlawless.agentengine.game.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GameSnapshot(
        UUID gameId,
        int requiredPlayerCount,
        boolean ready,
        List<GameParticipant> participants,
        List<String> actionTypes,
        int turn,
        Map<String, Object> state,
        Instant createdAt,
        Instant updatedAt
) {
}
