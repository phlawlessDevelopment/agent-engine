package dev.phlawless.agentengine.game.domain;

import java.util.Map;

public record ActionSchema(
        String type,
        String description,
        Map<String, ValueSchema> payload
) {
}
