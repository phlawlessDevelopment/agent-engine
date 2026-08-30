package dev.phlawless.agentengine.examples.tictactoe;

import dev.phlawless.agentengine.game.domain.GameState;

import java.util.List;
import java.util.Map;

public record TicTacToeState(
        List<String> board,
        String currentPlayer,
        String status,
        String winner
) implements GameState {
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String WINNER = "WINNER";
    public static final String DRAW = "DRAW";
    public static final String PLAYER_X = "X";
    public static final String PLAYER_O = "O";

    public static TicTacToeState fresh() {
        return new TicTacToeState(List.of("", "", "", "", "", "", "", "", ""), PLAYER_X, IN_PROGRESS, "");
    }

    @Override
    public Map<String, Object> toObservable() {
        return Map.of(
                "board", board,
                "currentPlayer", currentPlayer,
                "status", status,
                "winner", winner);
    }
}
