package dev.phlawless.agentengine.examples.chess;

import dev.phlawless.agentengine.game.domain.ActionSchema;
import dev.phlawless.agentengine.game.domain.Command;
import dev.phlawless.agentengine.game.domain.EventSchema;
import dev.phlawless.agentengine.game.domain.EventSpec;
import dev.phlawless.agentengine.game.domain.GameRules;
import dev.phlawless.agentengine.game.domain.GameRulesDescription;
import dev.phlawless.agentengine.game.domain.GameState;
import dev.phlawless.agentengine.game.domain.PlayerContext;
import dev.phlawless.agentengine.game.domain.RuleResult;
import dev.phlawless.agentengine.game.domain.ValueSchema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ChessRules implements GameRules {
    public static final String MOVE_ACTION = "MOVE";
    public static final String MOVE_PLAYED_EVENT = "MOVE_PLAYED";
    public static final String CHECK_EVENT = "CHECK";
    public static final String GAME_WON_EVENT = "GAME_WON";
    public static final String GAME_DRAWN_EVENT = "GAME_DRAWN";

    private static final List<String> PROMOTION_VALUES = List.of("QUEEN", "ROOK", "BISHOP", "KNIGHT");

    @Override
    public int requiredPlayerCount() {
        return 2;
    }

    @Override
    public Set<String> actionTypes() {
        return Set.of(MOVE_ACTION);
    }

    @Override
    public GameState initialState() {
        return ChessState.fresh();
    }

    @Override
    public RuleResult evaluate(GameState state, Command command, PlayerContext player, int turn, Instant now) {
        if (!(state instanceof ChessState chessState)) {
            return RuleResult.reject("Invalid state for chess");
        }
        if (!MOVE_ACTION.equals(command.type())) {
            return RuleResult.reject("Unknown action: " + command.type());
        }
        if (!ChessState.IN_PROGRESS.equals(chessState.status())) {
            return RuleResult.reject("Game is over");
        }

        Color actorColor = colorForSeat(player.seat());
        if (actorColor == null) {
            return RuleResult.reject("Unsupported player seat: " + player.seat());
        }

        String from = asString(command.payload().get("from"));
        String to = asString(command.payload().get("to"));
        if (from == null || to == null) {
            return RuleResult.reject("Move must include from and to squares");
        }

        Square fromSquare = Square.parse(from);
        Square toSquare = Square.parse(to);
        if (fromSquare == null || toSquare == null) {
            return RuleResult.reject("Squares must be in algebraic notation (a1-h8)");
        }

        ChessPiece piece = chessState.board().get(fromSquare.notation());
        if (piece == null) {
            return RuleResult.reject("No piece at " + from);
        }
        if (piece.color() != actorColor) {
            return RuleResult.reject("You may only move your own pieces");
        }
        if (!actorColor.marker().equals(chessState.currentPlayer())) {
            return RuleResult.reject("It is not your turn");
        }

        ChessPiece captured = chessState.board().get(toSquare.notation());
        PieceType requestedPromotion = parsePromotion(command.payload().get("promotion"));
        if (requestedPromotion != null && !isPromotionRequestValid(piece, toSquare, requestedPromotion)) {
            return RuleResult.reject("Invalid promotion target");
        }

        PieceType promotion = determinePromotion(piece, toSquare, requestedPromotion);
        if (!isLegalMove(chessState.board(), fromSquare, toSquare, piece, promotion)) {
            return RuleResult.reject("Move is not legal");
        }

        Map<String, ChessPiece> nextBoard = applyMove(chessState.board(), fromSquare.notation(), toSquare.notation(), piece, promotion);
        if (kingInCheck(nextBoard, actorColor)) {
            return RuleResult.reject("Move would leave your king in check");
        }

        Color opponent = actorColor.opponent();
        boolean opponentInCheck = kingInCheck(nextBoard, opponent);
        boolean opponentHasMove = hasLegalMoves(nextBoard, opponent);

        List<EventSpec> events = new ArrayList<>();
        events.add(new EventSpec(MOVE_PLAYED_EVENT, Map.of(
                "actorSeat", Integer.toString(player.seat()),
                "actorColor", actorColor.marker(),
                "from", fromSquare.notation(),
                "to", toSquare.notation(),
                "piece", piece.notation(),
                "captured", captured == null ? "" : captured.notation(),
                "promotion", promotion == null ? "" : promotion.name())));

        if (opponentInCheck) {
            events.add(new EventSpec(CHECK_EVENT, Map.of("attackedColor", opponent.marker())));
        }

        String nextStatus = ChessState.IN_PROGRESS;
        String winner = "";
        if (opponentInCheck && !opponentHasMove) {
            nextStatus = ChessState.CHECKMATE;
            winner = actorColor.marker();
            events.add(new EventSpec(GAME_WON_EVENT, Map.of("winner", winner, "reason", "CHECKMATE")));
        } else if (!opponentInCheck && !opponentHasMove) {
            nextStatus = ChessState.STALEMATE;
            events.add(new EventSpec(GAME_DRAWN_EVENT, Map.of("reason", "STALEMATE")));
        }

        ChessState nextState = new ChessState(nextBoard, opponent.marker(), nextStatus, winner);
        return RuleResult.accept(nextState, events);
    }

    @Override
    public GameRulesDescription describe() {
        return new GameRulesDescription(
                "Chess",
                "Core chess with legal movement, captures, checks, checkmate, stalemate, and pawn promotions (no castling or en passant).",
                requiredPlayerCount(),
                List.of(new ActionSchema(
                        MOVE_ACTION,
                        "Move one piece from one square to another using algebraic notation.",
                        Map.of(
                                "from", new ValueSchema("string", true, "Source square (a1-h8).", Map.of("pattern", "[a-h][1-8]")),
                                "to", new ValueSchema("string", true, "Destination square (a1-h8).", Map.of("pattern", "[a-h][1-8]")),
                                "promotion", new ValueSchema("string", false, "Piece type for pawn promotion (QUEEN, ROOK, BISHOP, KNIGHT).", Map.of("enum", PROMOTION_VALUES))
                        )
                )),
                Map.of(
                        "board", new ValueSchema("map<string,string>", true, "64-square map associating squares with piece notation (e.g. \"wK\").", Map.of("size", 64)),
                        "currentPlayer", new ValueSchema("string", true, "Next player (WHITE or BLACK).", Map.of("enum", List.of(Color.WHITE.marker(), Color.BLACK.marker()))),
                        "status", new ValueSchema("string", true, "Current game status.", Map.of("enum", List.of(ChessState.IN_PROGRESS, ChessState.CHECKMATE, ChessState.STALEMATE))),
                        "winner", new ValueSchema("string", true, "Winning color when the game ends, otherwise empty string.", Map.of("enum", List.of("", Color.WHITE.marker(), Color.BLACK.marker())))
                ),
                List.of(
                        new EventSchema(
                                MOVE_PLAYED_EVENT,
                                "A move was executed.",
                                Map.of(
                                        "actorSeat", new ValueSchema("string", true, "Seat number of the moving player.", Map.of()),
                                        "actorColor", new ValueSchema("string", true, "Color associated with the actor.", Map.of("enum", List.of(Color.WHITE.marker(), Color.BLACK.marker()))),
                                        "from", new ValueSchema("string", true, "Source square.", Map.of()),
                                        "to", new ValueSchema("string", true, "Destination square.", Map.of()),
                                        "piece", new ValueSchema("string", true, "Moved piece notation.", Map.of()),
                                        "captured", new ValueSchema("string", true, "Notation for a captured piece or empty string.", Map.of()),
                                        "promotion", new ValueSchema("string", true, "Promotion target or empty string.", Map.of())
                                )
                        ),
                        new EventSchema(
                                CHECK_EVENT,
                                "A king is in check.",
                                Map.of("attackedColor", new ValueSchema("string", true, "King color being attacked.", Map.of()))
                        ),
                        new EventSchema(
                                GAME_WON_EVENT,
                                "The game ended with a winner.",
                                Map.of(
                                        "winner", new ValueSchema("string", true, "Winning color.", Map.of("enum", List.of(Color.WHITE.marker(), Color.BLACK.marker()))),
                                        "reason", new ValueSchema("string", true, "Reason for the win (CHECKMATE).", Map.of())
                                )
                        ),
                        new EventSchema(
                                GAME_DRAWN_EVENT,
                                "The game ended in a draw.",
                                Map.of("reason", new ValueSchema("string", true, "Explanation for the draw (STALEMATE).", Map.of()))
                        )
                )
        );
    }

    private static Color colorForSeat(int seat) {
        return switch (seat) {
            case 0 -> Color.WHITE;
            case 1 -> Color.BLACK;
            default -> null;
        };
    }

    private static String asString(Object raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.toString().trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static PieceType parsePromotion(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.toString().trim().toUpperCase();
        if (!PROMOTION_VALUES.contains(value)) {
            return null;
        }
        return PieceType.valueOf(value);
    }

    private static PieceType determinePromotion(ChessPiece piece, Square destination, PieceType requested) {
        if (piece.type() != PieceType.PAWN || !isPromotionRank(destination, piece.color())) {
            return null;
        }
        return requested == null ? PieceType.QUEEN : requested;
    }

    private static boolean isPromotionRequestValid(ChessPiece piece, Square destination, PieceType requested) {
        if (piece.type() != PieceType.PAWN) {
            return false;
        }
        if (!isPromotionRank(destination, piece.color())) {
            return false;
        }
        return requested != null;
    }

    private static boolean isPromotionRank(Square destination, Color color) {
        int targetRank = destination.rank();
        return color == Color.WHITE ? targetRank == 7 : targetRank == 0;
    }

    private boolean isLegalMove(Map<String, ChessPiece> board, Square from, Square to, ChessPiece piece, PieceType promotion) {
        if (from.equals(to)) {
            return false;
        }
        ChessPiece occupant = board.get(to.notation());
        if (occupant != null && occupant.color() == piece.color()) {
            return false;
        }

        int fileDiff = to.file() - from.file();
        int rankDiff = to.rank() - from.rank();
        int absFile = Math.abs(fileDiff);
        int absRank = Math.abs(rankDiff);

        return switch (piece.type()) {
            case PAWN -> validatePawnMove(board, from, to, piece, occupant, fileDiff, rankDiff);
            case KNIGHT -> (absFile == 1 && absRank == 2) || (absFile == 2 && absRank == 1);
            case BISHOP -> absFile == absRank && isPathClear(board, from, to, Integer.signum(fileDiff), Integer.signum(rankDiff));
            case ROOK -> (fileDiff == 0 || rankDiff == 0) && isPathClear(board, from, to, Integer.signum(fileDiff), Integer.signum(rankDiff));
            case QUEEN -> (absFile == absRank || fileDiff == 0 || rankDiff == 0) && isPathClear(board, from, to, Integer.signum(fileDiff), Integer.signum(rankDiff));
            case KING -> Math.max(absFile, absRank) == 1;
        };
    }

    private boolean validatePawnMove(Map<String, ChessPiece> board, Square from, Square to, ChessPiece piece, ChessPiece occupant, int fileDiff, int rankDiff) {
        int direction = piece.color() == Color.WHITE ? 1 : -1;
        if (fileDiff == 0) {
            if (rankDiff == direction && occupant == null) {
                return true;
            }
            if (rankDiff == 2 * direction && occupant == null) {
                int startRank = piece.color() == Color.WHITE ? 1 : 6;
                if (from.rank() != startRank) {
                    return false;
                }
                Square between = from.offset(0, direction);
                return between != null && board.get(between.notation()) == null;
            }
            return false;
        }
        return Math.abs(fileDiff) == 1 && rankDiff == direction && occupant != null && occupant.color() != piece.color();
    }

    private boolean isPathClear(Map<String, ChessPiece> board, Square from, Square to, int fileStep, int rankStep) {
        if (fileStep == 0 && rankStep == 0) {
            return false;
        }
        Square cursor = from.offset(fileStep, rankStep);
        while (cursor != null) {
            if (cursor.equals(to)) {
                return true;
            }
            if (board.containsKey(cursor.notation())) {
                return false;
            }
            cursor = cursor.offset(fileStep, rankStep);
        }
        return false;
    }

    private Map<String, ChessPiece> applyMove(Map<String, ChessPiece> board, String from, String to, ChessPiece piece, PieceType promotion) {
        Map<String, ChessPiece> next = new LinkedHashMap<>(board);
        next.remove(from);
        ChessPiece moved = piece;
        if (promotion != null) {
            moved = new ChessPiece(piece.color(), promotion);
        }
        next.put(to, moved);
        return next;
    }

    private boolean kingInCheck(Map<String, ChessPiece> board, Color color) {
        String kingSquare = findKingSquare(board, color);
        if (kingSquare == null) {
            return true;
        }
        return squareAttacked(board, kingSquare, color.opponent());
    }

    private boolean squareAttacked(Map<String, ChessPiece> board, String square, Color attacker) {
        Square target = Square.parse(square);
        if (target == null) {
            return false;
        }
        for (Map.Entry<String, ChessPiece> entry : board.entrySet()) {
            ChessPiece piece = entry.getValue();
            if (piece.color() != attacker) {
                continue;
            }
            Square from = Square.parse(entry.getKey());
            if (from == null) {
                continue;
            }
            PieceType promotion = determinePromotion(piece, target, null);
            if (isLegalMove(board, from, target, piece, promotion)) {
                return true;
            }
        }
        return false;
    }

    private String findKingSquare(Map<String, ChessPiece> board, Color color) {
        for (Map.Entry<String, ChessPiece> entry : board.entrySet()) {
            ChessPiece piece = entry.getValue();
            if (piece.color() == color && piece.type() == PieceType.KING) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean hasLegalMoves(Map<String, ChessPiece> board, Color color) {
        for (Map.Entry<String, ChessPiece> entry : board.entrySet()) {
            if (entry.getValue().color() != color) {
                continue;
            }
            Square from = Square.parse(entry.getKey());
            if (from == null) {
                continue;
            }
            for (String destination : ChessBoard.SQUARES) {
                Square to = Square.parse(destination);
                if (to == null || from.equals(to)) {
                    continue;
                }
                PieceType promotion = determinePromotion(entry.getValue(), to, null);
                if (!isLegalMove(board, from, to, entry.getValue(), promotion)) {
                    continue;
                }
                Map<String, ChessPiece> candidate = applyMove(board, from.notation(), to.notation(), entry.getValue(), promotion);
                if (!kingInCheck(candidate, color)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final class Square {
        private final int file;
        private final int rank;

        private Square(int file, int rank) {
            this.file = file;
            this.rank = rank;
        }

        static Square parse(String notation) {
            if (notation == null || notation.length() != 2) {
                return null;
            }
            char fileChar = Character.toLowerCase(notation.charAt(0));
            char rankChar = notation.charAt(1);
            int file = fileChar - 'a';
            int rank = rankChar - '1';
            if (file < 0 || file >= 8 || rank < 0 || rank >= 8) {
                return null;
            }
            return new Square(file, rank);
        }

        Square offset(int fileDelta, int rankDelta) {
            int nextFile = file + fileDelta;
            int nextRank = rank + rankDelta;
            if (nextFile < 0 || nextFile >= 8 || nextRank < 0 || nextRank >= 8) {
                return null;
            }
            return new Square(nextFile, nextRank);
        }

        int file() {
            return file;
        }

        int rank() {
            return rank;
        }

        String notation() {
            return ChessBoard.FILES[file] + ChessBoard.RANKS[rank];
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Square)) {
                return false;
            }
            Square square = (Square) o;
            return file == square.file && rank == square.rank;
        }

        @Override
        public int hashCode() {
            return Objects.hash(file, rank);
        }
    }
}
