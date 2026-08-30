package dev.phlawless.agentengine.game.domain;

import java.util.Map;

public interface GameState {
    Map<String, Object> toObservable();
}
