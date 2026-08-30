package dev.phlawless.agentengine.game.domain;

import java.util.Map;

public record ValueSchema(
        String type,
        boolean required,
        String description,
        Map<String, Object> constraints
) {
}
