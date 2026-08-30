package dev.phlawless.agentengine.game.application;

public final class UnknownGameTypeException extends RuntimeException {
    public UnknownGameTypeException(String gameType) {
        super("Unknown game type: " + gameType);
    }
}
