package dev.phlawless.agentengine.game.api;

import java.util.Map;

public record ValueSchemaResponse(
        String type,
        boolean required,
        String description,
        Map<String, Object> constraints
) {
}
