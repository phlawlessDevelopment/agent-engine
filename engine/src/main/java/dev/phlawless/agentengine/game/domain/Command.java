package dev.phlawless.agentengine.game.domain;

import java.util.Map;

public record Command(String type, Map<String, Object> payload) {
    public Command {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Command type must not be blank");
        }
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
