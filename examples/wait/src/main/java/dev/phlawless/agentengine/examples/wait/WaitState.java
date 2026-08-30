package dev.phlawless.agentengine.examples.wait;

import dev.phlawless.agentengine.game.domain.GameState;

import java.util.Map;

public record WaitState() implements GameState {
    @Override
    public Map<String, Object> toObservable() {
        return Map.of();
    }
}
