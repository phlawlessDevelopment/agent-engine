package dev.phlawless.agentengine.game.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public interface GameRules {
    int requiredPlayerCount();

    Set<String> actionTypes();

    GameState initialState();

    RuleResult evaluate(GameState state, Command command, PlayerContext player, int turn, Instant now);

    default GameRulesDescription describe() {
        return new GameRulesDescription(
                "Game",
                "No structured rules description provided by this game.",
                requiredPlayerCount(),
                actionTypes().stream()
                        .sorted()
                        .map(type -> new ActionSchema(type, "", Map.of()))
                        .toList(),
                Map.of(),
                java.util.List.of());
    }
}
