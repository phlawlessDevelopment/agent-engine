package dev.phlawless.agentengine.examples.starter;

import dev.phlawless.agentengine.game.domain.GameState;

import java.util.Map;

public record StarterGameState(int moveCount, String status) implements GameState {
    public static final String IN_PROGRESS = "IN_PROGRESS";

    public static StarterGameState fresh() {
        return new StarterGameState(0, IN_PROGRESS);
    }

    @Override
    public Map<String, Object> toObservable() {
        return Map.of(
                "moveCount", moveCount,
                "status", status);
    }
}
