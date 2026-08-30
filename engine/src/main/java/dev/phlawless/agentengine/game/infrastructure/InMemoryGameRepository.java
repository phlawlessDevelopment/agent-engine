package dev.phlawless.agentengine.game.infrastructure;

import dev.phlawless.agentengine.game.application.GameRepository;
import dev.phlawless.agentengine.game.domain.Game;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryGameRepository implements GameRepository {
    private final ConcurrentMap<UUID, Game> games = new ConcurrentHashMap<>();

    @Override
    public Optional<Game> findById(UUID gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    @Override
    public void save(Game game) {
        games.put(game.getId(), game);
    }
}
