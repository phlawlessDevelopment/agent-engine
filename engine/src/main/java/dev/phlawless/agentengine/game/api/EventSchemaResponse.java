package dev.phlawless.agentengine.game.api;

import java.util.Map;

public record EventSchemaResponse(
        String type,
        String description,
        Map<String, ValueSchemaResponse> details
) {
}
