package dev.phlawless.agentengine.game.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Game {
    private final UUID id;
    private final GameRules rules;
    private final Instant createdAt;
    private Instant updatedAt;
    private int turn;
    private long nextEventSequence;
    private final List<GameEvent> events;
    private GameState state;

    private Game(UUID id, GameRules rules, Instant createdAt) {
        this.id = id;
        this.rules = rules;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.turn = 0;
        this.nextEventSequence = 1;
        this.events = new ArrayList<>();
    }

    public static Game create(UUID id, GameRules rules, Instant now) {
        Game game = new Game(id, rules, now);
        game.state = rules.initialState();
        game.appendEvent("GAME_CREATED", now, Map.of());
        return game;
    }

    public RuleResult apply(Command command, Instant now) {
        RuleResult result = rules.evaluate(state, command, turn, now);
        if (!result.accepted()) {
            return result;
        }
        state = result.nextState();
        updatedAt = now;
        turn += 1;
        for (EventSpec event : result.events()) {
            appendEvent(event.type(), now, event.details());
        }
        return result;
    }

    public UUID getId() {
        return id;
    }

    public GameSnapshot snapshot() {
        return new GameSnapshot(
                id,
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

    private void appendEvent(String type, Instant now, Map<String, String> details) {
        events.add(new GameEvent(nextEventSequence, turn, type, now, details));
        nextEventSequence += 1;
    }
}
