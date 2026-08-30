package dev.phlawless.agentengine.examples.starter;

import dev.phlawless.agentengine.game.domain.Command;
import dev.phlawless.agentengine.game.domain.EventSpec;
import dev.phlawless.agentengine.game.domain.EventSchema;
import dev.phlawless.agentengine.game.domain.GameRulesDescription;
import dev.phlawless.agentengine.game.domain.GameRules;
import dev.phlawless.agentengine.game.domain.GameState;
import dev.phlawless.agentengine.game.domain.PlayerContext;
import dev.phlawless.agentengine.game.domain.RuleResult;
import dev.phlawless.agentengine.game.domain.ActionSchema;
import dev.phlawless.agentengine.game.domain.ValueSchema;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StarterGameRules implements GameRules {
    public static final String TAKE_TURN_ACTION = "TAKE_TURN";
    public static final String TURN_TAKEN_EVENT = "TURN_TAKEN";

    @Override
    public int requiredPlayerCount() {
        return 1;
    }

    @Override
    public Set<String> actionTypes() {
        return Set.of(TAKE_TURN_ACTION);
    }

    @Override
    public GameState initialState() {
        return StarterGameState.fresh();
    }

    @Override
    public RuleResult evaluate(GameState state, Command command, PlayerContext player, int turn, Instant now) {
        if (!(state instanceof StarterGameState starterState)) {
            return RuleResult.reject("Invalid state for starter game");
        }
        if (!TAKE_TURN_ACTION.equals(command.type())) {
            return RuleResult.reject("Unknown action: " + command.type());
        }

        int nextMoveCount = starterState.moveCount() + 1;
        StarterGameState nextState = new StarterGameState(nextMoveCount, StarterGameState.IN_PROGRESS);
        EventSpec event = new EventSpec(
                TURN_TAKEN_EVENT,
                Map.of(
                        "turn", Integer.toString(turn + 1),
                        "moveCount", Integer.toString(nextMoveCount),
                        "at", now.toString()));

        return RuleResult.accept(nextState, List.of(event));
    }

    @Override
    public GameRulesDescription describe() {
        return new GameRulesDescription(
                "Starter",
                "Single-player starter game that increments move count each turn.",
                requiredPlayerCount(),
                List.of(new ActionSchema(
                        TAKE_TURN_ACTION,
                        "Increment move count and emit a TURN_TAKEN event.",
                        Map.of()
                )),
                Map.of(
                        "moveCount", new ValueSchema("integer", true, "Total accepted turns so far.", Map.of("min", 0)),
                        "status", new ValueSchema(
                                "string",
                                true,
                                "Current game status.",
                                Map.of("enum", List.of(StarterGameState.IN_PROGRESS))
                        )
                ),
                List.of(new EventSchema(
                        TURN_TAKEN_EVENT,
                        "A turn was accepted.",
                        Map.of(
                                "turn", new ValueSchema("string", true, "New turn number as a string.", Map.of()),
                                "moveCount", new ValueSchema("string", true, "New move count as a string.", Map.of()),
                                "at", new ValueSchema("string", true, "UTC timestamp for the accepted turn.", Map.of("format", "date-time"))
                        )
                ))
        );
    }
}
