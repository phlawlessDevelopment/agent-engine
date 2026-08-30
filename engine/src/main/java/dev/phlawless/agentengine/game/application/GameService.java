package dev.phlawless.agentengine.game.application;

import dev.phlawless.agentengine.game.domain.Command;
import dev.phlawless.agentengine.game.domain.Game;
import dev.phlawless.agentengine.game.domain.GameEvent;
import dev.phlawless.agentengine.game.domain.GameRules;
import dev.phlawless.agentengine.game.domain.GameSnapshot;
import dev.phlawless.agentengine.game.domain.RuleResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class GameService {
    private final GameRepository gameRepository;
    private final GameRulesRegistry rulesRegistry;
    private final Clock clock;
    private final String defaultGameType;

    public GameService(
            GameRepository gameRepository,
            GameRulesRegistry rulesRegistry,
            Clock clock,
            @Value("${agent-engine.default-game-type}") String defaultGameType) {
        this.gameRepository = gameRepository;
        this.rulesRegistry = rulesRegistry;
        this.clock = clock;
        this.defaultGameType = defaultGameType;
    }

    public GameSnapshot createGame() {
        return createGame(defaultGameType);
    }

    public GameSnapshot createGame(String gameType) {
        String resolvedType = gameType == null ? defaultGameType : gameType;
        GameRules rules = rulesRegistry.require(resolvedType);
        Game game = Game.create(UUID.randomUUID(), rules, Instant.now(clock));
        gameRepository.save(game);
        return game.snapshot();
    }

    public GameSnapshot getState(UUID gameId) {
        return withGame(gameId, Game::snapshot);
    }

    public ActionExecutionResult executeAction(UUID gameId, Command command) {
        return withGame(gameId, game -> {
            long latestBefore = game.latestEventSequence();
            RuleResult result = game.apply(command, Instant.now(clock));
            gameRepository.save(game);
            List<GameEvent> emitted = game.eventsAfter(latestBefore);
            return new ActionExecutionResult(result.accepted(), result.message(), game.snapshot(), emitted);
        });
    }

    public List<GameEvent> getEvents(UUID gameId, long afterSequence) {
        if (afterSequence < 0) {
            throw new IllegalArgumentException("afterSequence must be >= 0");
        }

        return withGame(gameId, game -> game.eventsAfter(afterSequence));
    }

    private <T> T withGame(UUID gameId, java.util.function.Function<Game, T> operation) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));
        synchronized (game) {
            return operation.apply(game);
        }
    }

    public record ActionExecutionResult(
            boolean accepted,
            String message,
            GameSnapshot snapshot,
            List<GameEvent> emittedEvents
    ) {
    }
}
