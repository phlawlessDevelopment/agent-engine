package dev.phlawless.agentengine.game.application;

import dev.phlawless.agentengine.game.domain.Game;

import java.util.Optional;
import java.util.UUID;

public interface GameRepository {
    Optional<Game> findById(UUID gameId);

    void save(Game game);
}
