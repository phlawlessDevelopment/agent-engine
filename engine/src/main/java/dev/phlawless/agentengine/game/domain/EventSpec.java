package dev.phlawless.agentengine.game.domain;

import java.util.Map;

public record EventSpec(String type, Map<String, String> details) {
    public EventSpec {
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
