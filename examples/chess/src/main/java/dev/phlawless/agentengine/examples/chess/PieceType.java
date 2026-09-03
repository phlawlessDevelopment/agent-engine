package dev.phlawless.agentengine.examples.chess;

public enum PieceType {
    KING,
    QUEEN,
    ROOK,
    BISHOP,
    KNIGHT,
    PAWN;

    public boolean isSliding() {
        return this == QUEEN || this == ROOK || this == BISHOP;
    }
}
