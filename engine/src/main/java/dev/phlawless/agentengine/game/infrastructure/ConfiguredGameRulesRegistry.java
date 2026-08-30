package dev.phlawless.agentengine.game.infrastructure;

import dev.phlawless.agentengine.game.application.GameRulesRegistry;
import dev.phlawless.agentengine.game.application.UnknownGameTypeException;
import dev.phlawless.agentengine.game.domain.GameRules;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class ConfiguredGameRulesRegistry implements GameRulesRegistry {
    private final Map<String, GameRules> rulesByType;

    public ConfiguredGameRulesRegistry(List<GameRules> modules) {
        this.rulesByType = modules.stream()
                .collect(Collectors.toUnmodifiableMap(GameRules::gameType, Function.identity()));
    }

    @Override
    public GameRules require(String gameType) {
        if (gameType == null) {
            throw new UnknownGameTypeException("(null)");
        }
        GameRules rules = rulesByType.get(gameType);
        if (rules == null) {
            throw new UnknownGameTypeException(gameType);
        }
        return rules;
    }
}
