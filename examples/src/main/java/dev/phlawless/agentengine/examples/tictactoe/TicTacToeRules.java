package dev.phlawless.agentengine.examples.tictactoe;

import dev.phlawless.agentengine.game.domain.Command;
import dev.phlawless.agentengine.game.domain.EventSpec;
import dev.phlawless.agentengine.game.domain.GameRules;
import dev.phlawless.agentengine.game.domain.GameState;
import dev.phlawless.agentengine.game.domain.RuleResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class TicTacToeRules implements GameRules {
    public static final String GAME_TYPE = "tictactoe";
    public static final String PLACE_MARKER_ACTION = "PLACE_MARKER";
    public static final String MARKER_PLACED_EVENT = "MARKER_PLACED";
    public static final String GAME_WON_EVENT = "GAME_WON";
    public static final String GAME_DRAWN_EVENT = "GAME_DRAWN";

    private static final int BOARD_SIZE = 9;
    private static final int[][] WIN_LINES = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {2, 4, 6}
    };

    @Override
    public String gameType() {
        return GAME_TYPE;
    }

    @Override
    public Set<String> actionTypes() {
        return Set.of(PLACE_MARKER_ACTION);
    }

    @Override
    public GameState initialState() {
        return TicTacToeState.fresh();
    }

    @Override
    public RuleResult evaluate(GameState state, Command command, int turn, Instant now) {
        if (!(state instanceof TicTacToeState gameState)) {
            return RuleResult.reject("Invalid state for tictactoe");
        }
        if (!PLACE_MARKER_ACTION.equals(command.type())) {
            return RuleResult.reject("Unknown action: " + command.type());
        }
        if (!TicTacToeState.IN_PROGRESS.equals(gameState.status())) {
            return RuleResult.reject("Game is over");
        }

        Integer position = parsePosition(command);
        if (position == null) {
            return RuleResult.reject("Position must be an integer between 0 and 8");
        }
        if (!gameState.board().get(position).isEmpty()) {
            return RuleResult.reject("Cell already occupied: " + position);
        }

        String marker = gameState.currentPlayer();
        List<String> nextBoard = new ArrayList<>(gameState.board());
        nextBoard.set(position, marker);

        List<EventSpec> events = new ArrayList<>();
        events.add(new EventSpec(MARKER_PLACED_EVENT, Map.of(
                "position", Integer.toString(position),
                "marker", marker)));

        if (hasWinningLine(nextBoard, marker)) {
            events.add(new EventSpec(GAME_WON_EVENT, Map.of("winner", marker)));
            return RuleResult.accept(
                    new TicTacToeState(
                            List.copyOf(nextBoard),
                            opponent(marker),
                            TicTacToeState.WINNER,
                            marker),
                    events);
        }
        if (!nextBoard.contains("")) {
            events.add(new EventSpec(GAME_DRAWN_EVENT, Map.of()));
            return RuleResult.accept(
                    new TicTacToeState(
                            List.copyOf(nextBoard),
                            opponent(marker),
                            TicTacToeState.DRAW,
                            ""),
                    events);
        }

        return RuleResult.accept(
                new TicTacToeState(
                        List.copyOf(nextBoard),
                        opponent(marker),
                        TicTacToeState.IN_PROGRESS,
                        ""),
                events);
    }

    private Integer parsePosition(Command command) {
        Object raw = command.payload().get("position");
        if (raw == null) {
            return null;
        }
        try {
            int position = Integer.parseInt(String.valueOf(raw).trim());
            return position >= 0 && position < BOARD_SIZE ? position : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean hasWinningLine(List<String> board, String marker) {
        for (int[] line : WIN_LINES) {
            if (board.get(line[0]).equals(marker)
                    && board.get(line[1]).equals(marker)
                    && board.get(line[2]).equals(marker)) {
                return true;
            }
        }
        return false;
    }

    private String opponent(String marker) {
        return TicTacToeState.PLAYER_X.equals(marker) ? TicTacToeState.PLAYER_O : TicTacToeState.PLAYER_X;
    }
}
