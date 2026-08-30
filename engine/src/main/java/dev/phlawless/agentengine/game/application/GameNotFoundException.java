package dev.phlawless.agentengine.game.application;

import java.util.UUID;

public final class GameNotFoundException extends RuntimeException {
    public GameNotFoundException(UUID gameId) {
        super("Game not found: " + gameId);
    }
}
