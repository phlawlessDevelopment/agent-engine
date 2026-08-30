package dev.phlawless.agentengine.game.domain;

import java.util.Map;

public record EventSchema(
        String type,
        String description,
        Map<String, ValueSchema> details
) {
}
