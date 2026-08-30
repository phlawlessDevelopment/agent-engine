package dev.phlawless.agentengine.game.domain;

import dev.phlawless.agentengine.account.domain.AccountIdentity;
import dev.phlawless.agentengine.game.application.GameFullException;
import dev.phlawless.agentengine.game.application.GameNotReadyException;
import dev.phlawless.agentengine.game.application.InvalidPlayerCountException;
import dev.phlawless.agentengine.game.application.NotGameParticipantException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Game {
    private final UUID id;
    private final GameRules rules;
    private final int requiredPlayerCount;
    private final Instant createdAt;
    private Instant updatedAt;
    private int turn;
    private long nextEventSequence;
    private final List<GameEvent> events;
    private final List<GameParticipant> participants;
    private GameState state;

    private Game(UUID id, GameRules rules, int requiredPlayerCount, Instant createdAt) {
        this.id = id;
        this.rules = rules;
        this.requiredPlayerCount = requiredPlayerCount;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.turn = 0;
        this.nextEventSequence = 1;
        this.events = new ArrayList<>();
        this.participants = new ArrayList<>();
    }

    public static Game create(UUID id, GameRules rules, AccountIdentity creator, Instant now) {
        int requiredPlayerCount = rules.requiredPlayerCount();
        if (requiredPlayerCount < 1 || requiredPlayerCount > 32) {
            throw new InvalidPlayerCountException(requiredPlayerCount);
        }

        Game game = new Game(id, rules, requiredPlayerCount, now);
        game.state = rules.initialState();
        GameParticipant creatorParticipant = new GameParticipant(creator.accountId(), creator.username(), 0);
        game.participants.add(creatorParticipant);
        game.appendEvent("GAME_CREATED", now, creatorParticipant, Map.of("requiredPlayerCount", Integer.toString(requiredPlayerCount)));
        return game;
    }

    public GameParticipant join(AccountIdentity account, Instant now) {
        GameParticipant existing = findParticipant(account.accountId());
        if (existing != null) {
            return existing;
        }
        if (participants.size() >= requiredPlayerCount) {
            throw new GameFullException(id);
        }

        GameParticipant participant = new GameParticipant(account.accountId(), account.username(), participants.size());
        participants.add(participant);
        updatedAt = now;
        appendEvent("PLAYER_JOINED", now, participant, Map.of("seat", Integer.toString(participant.seat())));
        return participant;
    }

    public RuleResult apply(UUID accountId, Command command, Instant now) {
        GameParticipant participant = requireParticipant(accountId);
        if (!isReady()) {
            throw new GameNotReadyException(id);
        }

        RuleResult result = rules.evaluate(
                state,
                command,
                new PlayerContext(participant.accountId(), participant.seat()),
                turn,
                now);
        if (!result.accepted()) {
            return result;
        }
        state = result.nextState();
        updatedAt = now;
        turn += 1;
        for (EventSpec event : result.events()) {
            appendEvent(event.type(), now, participant, event.details());
        }
        return result;
    }

    public UUID getId() {
        return id;
    }

    public boolean hasParticipant(UUID accountId) {
        return findParticipant(accountId) != null;
    }

    public boolean isReady() {
        return participants.size() >= requiredPlayerCount;
    }

    public GameParticipant requireParticipant(UUID accountId) {
        GameParticipant participant = findParticipant(accountId);
        if (participant == null) {
            throw new NotGameParticipantException(id, accountId);
        }
        return participant;
    }

    public GameSnapshot snapshot() {
        return new GameSnapshot(
                id,
                requiredPlayerCount,
                isReady(),
                List.copyOf(participants),
                rules.actionTypes().stream().sorted().toList(),
                turn,
                state.toObservable(),
                createdAt,
                updatedAt);
    }

    public long latestEventSequence() {
        return nextEventSequence - 1;
    }

    public List<GameEvent> eventsAfter(long sequence) {
        return events.stream()
                .filter(event -> event.sequence() > sequence)
                .toList();
    }

    private GameParticipant findParticipant(UUID accountId) {
        for (GameParticipant participant : participants) {
            if (participant.accountId().equals(accountId)) {
                return participant;
            }
        }
        return null;
    }

    private void appendEvent(String type, Instant now, GameParticipant actor, Map<String, String> details) {
        events.add(new GameEvent(nextEventSequence, turn, type, now, actor.accountId(), actor.seat(), details));
        nextEventSequence += 1;
    }
}
