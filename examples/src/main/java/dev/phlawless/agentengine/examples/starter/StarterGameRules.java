package dev.phlawless.agentengine.examples.starter;

import dev.phlawless.agentengine.game.domain.Command;
import dev.phlawless.agentengine.game.domain.EventSpec;
import dev.phlawless.agentengine.game.domain.GameRules;
import dev.phlawless.agentengine.game.domain.GameState;
import dev.phlawless.agentengine.game.domain.PlayerContext;
import dev.phlawless.agentengine.game.domain.RuleResult;

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
}
