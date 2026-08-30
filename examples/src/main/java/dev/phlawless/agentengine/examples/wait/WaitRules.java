package dev.phlawless.agentengine.examples.wait;

import dev.phlawless.agentengine.game.domain.Command;
import dev.phlawless.agentengine.game.domain.EventSpec;
import dev.phlawless.agentengine.game.domain.GameRules;
import dev.phlawless.agentengine.game.domain.GameState;
import dev.phlawless.agentengine.game.domain.RuleResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class WaitRules implements GameRules {
    public static final String GAME_TYPE = "wait";
    public static final String WAIT_ACTION = "WAIT";
    public static final String TURN_ADVANCED_EVENT = "TURN_ADVANCED";

    @Override
    public String gameType() {
        return GAME_TYPE;
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
    public RuleResult evaluate(GameState state, Command command, int turn, Instant now) {
        if (!WAIT_ACTION.equals(command.type())) {
            return RuleResult.reject("Unknown action: " + command.type());
        }
        return RuleResult.accept(
                state,
                List.of(new EventSpec(TURN_ADVANCED_EVENT, Map.of("turn", Integer.toString(turn + 1)))));
    }
}
