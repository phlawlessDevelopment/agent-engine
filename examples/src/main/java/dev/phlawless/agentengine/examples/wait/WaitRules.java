package dev.phlawless.agentengine.examples.wait;

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

public class WaitRules implements GameRules {
    public static final String WAIT_ACTION = "WAIT";
    public static final String TURN_ADVANCED_EVENT = "TURN_ADVANCED";

    @Override
    public int requiredPlayerCount() {
        return 1;
    }

    @Override
    public Set<String> actionTypes() {
        return Set.of(WAIT_ACTION);
    }

    @Override
    public GameState initialState() {
        return new WaitState();
    }

    @Override
    public RuleResult evaluate(GameState state, Command command, PlayerContext player, int turn, Instant now) {
        if (!WAIT_ACTION.equals(command.type())) {
            return RuleResult.reject("Unknown action: " + command.type());
        }
        return RuleResult.accept(
                state,
                List.of(new EventSpec(TURN_ADVANCED_EVENT, Map.of("turn", Integer.toString(turn + 1)))));
    }

    @Override
    public GameRulesDescription describe() {
        return new GameRulesDescription(
                "Wait",
                "Single-player example game where each WAIT action increments turn.",
                requiredPlayerCount(),
                List.of(new ActionSchema(
                        WAIT_ACTION,
                        "Advance to the next turn.",
                        Map.of()
                )),
                Map.of(),
                List.of(new EventSchema(
                        TURN_ADVANCED_EVENT,
                        "Turn advanced by one.",
                        Map.of("turn", new ValueSchema("string", true, "New turn number as a string.", Map.of()))
                ))
        );
    }
}
