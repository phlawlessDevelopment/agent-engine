package dev.phlawless.agentengine.examples.chess;

public enum Color {
    WHITE,
    BLACK;

    public Color opponent() {
        return this == WHITE ? BLACK : WHITE;
    }

    public String marker() {
        return this == WHITE ? "WHITE" : "BLACK";
    }
}
