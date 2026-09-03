package dev.phlawless.agentengine.examples.chess;

public record ChessPiece(Color color, PieceType type) {

    public String notation() {
        String prefix = color == Color.WHITE ? "w" : "b";
        return prefix + switch (type) {
            case KING -> "K";
            case QUEEN -> "Q";
            case ROOK -> "R";
            case BISHOP -> "B";
            case KNIGHT -> "N";
            case PAWN -> "P";
        };
    }
}
