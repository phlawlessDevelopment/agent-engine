package dev.phlawless.agentengine.game.api;

import java.util.Map;

public record ActionSchemaResponse(
        String type,
        String description,
        Map<String, ValueSchemaResponse> payload
) {
}
