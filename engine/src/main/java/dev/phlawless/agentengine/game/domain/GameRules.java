package dev.phlawless.agentengine.game.domain;

import java.time.Instant;
import java.util.Set;

public interface GameRules {
    Set<String> actionTypes();

    GameState initialState();

    RuleResult evaluate(GameState state, Command command, int turn, Instant now);
}
