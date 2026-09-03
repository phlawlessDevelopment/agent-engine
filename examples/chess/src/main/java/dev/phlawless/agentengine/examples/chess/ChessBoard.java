package dev.phlawless.agentengine.examples.chess;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ChessBoard {

    public static final String[] FILES = {"a", "b", "c", "d", "e", "f", "g", "h"};
    public static final String[] RANKS = {"1", "2", "3", "4", "5", "6", "7", "8"};
    public static final List<String> SQUARES;

    static {
        List<String> squares = new ArrayList<>();
        for (String rank : RANKS) {
            for (String file : FILES) {
                squares.add(file + rank);
            }
        }
        SQUARES = List.copyOf(squares);
    }

    private static final PieceType[] BACK_RANK = {
            PieceType.ROOK,
            PieceType.KNIGHT,
            PieceType.BISHOP,
            PieceType.QUEEN,
            PieceType.KING,
            PieceType.BISHOP,
            PieceType.KNIGHT,
            PieceType.ROOK
    };

    private ChessBoard() {
    }

    public static Map<String, ChessPiece> initialSetup() {
        Map<String, ChessPiece> board = new LinkedHashMap<>();
        for (int fileIndex = 0; fileIndex < FILES.length; fileIndex++) {
            String file = FILES[fileIndex];
            board.put(file + "1", new ChessPiece(Color.WHITE, BACK_RANK[fileIndex]));
            board.put(file + "2", new ChessPiece(Color.WHITE, PieceType.PAWN));
            board.put(file + "7", new ChessPiece(Color.BLACK, PieceType.PAWN));
            board.put(file + "8", new ChessPiece(Color.BLACK, BACK_RANK[fileIndex]));
        }
        return board;
    }
}
