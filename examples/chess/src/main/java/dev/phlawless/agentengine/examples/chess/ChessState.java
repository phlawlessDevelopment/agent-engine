package dev.phlawless.agentengine.examples.chess;

import dev.phlawless.agentengine.game.domain.GameState;

import java.util.LinkedHashMap;
import java.util.Map;

public record ChessState(
        Map<String, ChessPiece> board,
        String currentPlayer,
        String status,
        String winner
) implements GameState {
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String CHECKMATE = "CHECKMATE";
    public static final String STALEMATE = "STALEMATE";

    public ChessState {
        board = Map.copyOf(board);
    }

    public static ChessState fresh() {
        return new ChessState(ChessBoard.initialSetup(), Color.WHITE.marker(), IN_PROGRESS, "");
    }

    @Override
    public Map<String, Object> toObservable() {
        Map<String, String> view = new LinkedHashMap<>();
        for (String square : ChessBoard.SQUARES) {
            ChessPiece piece = board.get(square);
            view.put(square, piece == null ? "" : piece.notation());
        }
        return Map.of(
                "board", view,
                "currentPlayer", currentPlayer,
                "status", status,
                "winner", winner);
    }
}
