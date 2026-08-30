package dev.phlawless.agentengine.game.application;

public final class InvalidPlayerCountException extends RuntimeException {
    public InvalidPlayerCountException(int requiredPlayerCount) {
        super("Invalid required player count: " + requiredPlayerCount);
    }
}
