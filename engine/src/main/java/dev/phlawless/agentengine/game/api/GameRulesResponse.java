package dev.phlawless.agentengine.game.api;

import java.util.List;
import java.util.Map;

public record GameRulesResponse(
        String game,
        String description,
        int requiredPlayerCount,
        List<ActionSchemaResponse> actions,
        Map<String, ValueSchemaResponse> observableState,
        List<EventSchemaResponse> events
) {
}
