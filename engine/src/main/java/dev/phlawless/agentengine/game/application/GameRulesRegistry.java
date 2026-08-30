package dev.phlawless.agentengine.game.application;

import dev.phlawless.agentengine.game.domain.GameRules;

public interface GameRulesRegistry {
    GameRules require(String gameType);
}
