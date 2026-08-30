package dev.phlawless.agentengine.game.domain;

import java.util.List;

public record RuleResult(
        boolean accepted,
        String message,
        GameState nextState,
        List<EventSpec> events
) {
    public static RuleResult reject(String message) {
        return new RuleResult(false, message, null, List.of());
    }

    public static RuleResult accept(GameState nextState, List<EventSpec> events) {
        return new RuleResult(true, "Action accepted", nextState, events);
    }
}
