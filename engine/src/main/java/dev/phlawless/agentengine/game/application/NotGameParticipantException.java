package dev.phlawless.agentengine.game.application;

import java.util.UUID;

public final class NotGameParticipantException extends RuntimeException {
    public NotGameParticipantException(UUID gameId, UUID accountId) {
        super("Account " + accountId + " is not a participant of game " + gameId);
    }
}
