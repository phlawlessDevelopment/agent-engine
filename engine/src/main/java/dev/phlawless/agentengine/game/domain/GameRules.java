package dev.phlawless.agentengine.game.domain;

import java.time.Instant;
import java.util.Set;

public interface GameRules {
    int requiredPlayerCount();

    Set<String> actionTypes();

    GameState initialState();

    RuleResult evaluate(GameState state, Command command, PlayerContext player, int turn, Instant now);
}
