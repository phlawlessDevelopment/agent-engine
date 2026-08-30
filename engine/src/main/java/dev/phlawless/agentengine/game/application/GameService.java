package dev.phlawless.agentengine.game.application;

import dev.phlawless.agentengine.account.domain.AccountIdentity;
import dev.phlawless.agentengine.game.domain.Command;
import dev.phlawless.agentengine.game.domain.Game;
import dev.phlawless.agentengine.game.domain.GameEvent;
import dev.phlawless.agentengine.game.domain.GameRules;
import dev.phlawless.agentengine.game.domain.GameSnapshot;
import dev.phlawless.agentengine.game.domain.RuleResult;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class GameService {
    private final GameRepository gameRepository;
    private final GameRules rules;
    private final Clock clock;

    public GameService(
            GameRepository gameRepository,
            GameRules rules,
            Clock clock) {
        this.gameRepository = gameRepository;
        this.rules = rules;
        this.clock = clock;
    }

    public GameSnapshot createGame(AccountIdentity creator) {
        Game game = Game.create(UUID.randomUUID(), rules, creator, Instant.now(clock));
        gameRepository.save(game);
        return game.snapshot();
    }

    public GameSnapshot joinGame(UUID gameId, AccountIdentity account) {
        return withGame(gameId, game -> {
            game.join(account, Instant.now(clock));
            gameRepository.save(game);
            return game.snapshot();
        });
    }

    public GameSnapshot getState(UUID gameId, UUID accountId) {
        return withGame(gameId, game -> {
            game.requireParticipant(accountId);
            return game.snapshot();
        });
    }

    public ActionExecutionResult executeAction(UUID gameId, UUID accountId, Command command) {
        return withGame(gameId, game -> {
            long latestBefore = game.latestEventSequence();
            RuleResult result = game.apply(accountId, command, Instant.now(clock));
            gameRepository.save(game);
            List<GameEvent> emitted = game.eventsAfter(latestBefore);
            return new ActionExecutionResult(result.accepted(), result.message(), game.snapshot(), emitted);
        });
    }

    public List<GameEvent> getEvents(UUID gameId, UUID accountId, long afterSequence) {
        if (afterSequence < 0) {
            throw new IllegalArgumentException("afterSequence must be >= 0");
        }

        return withGame(gameId, game -> {
            game.requireParticipant(accountId);
            return game.eventsAfter(afterSequence);
        });
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
