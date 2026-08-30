package dev.phlawless.agentengine.game.application;

import java.util.UUID;

public final class GameNotReadyException extends RuntimeException {
    public GameNotReadyException(UUID gameId) {
        super("Game is waiting for players: " + gameId);
    }
}
