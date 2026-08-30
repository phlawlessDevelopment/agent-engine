package dev.phlawless.agentengine.game.domain;

import java.util.List;
import java.util.Map;

public record GameRulesDescription(
        String game,
        String description,
        int requiredPlayerCount,
        List<ActionSchema> actions,
        Map<String, ValueSchema> observableState,
        List<EventSchema> events
) {
}
