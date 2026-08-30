package dev.phlawless.agentengine.game.application;

import java.util.UUID;

public final class GameFullException extends RuntimeException {
    public GameFullException(UUID gameId) {
        super("Game is full: " + gameId);
    }
}
