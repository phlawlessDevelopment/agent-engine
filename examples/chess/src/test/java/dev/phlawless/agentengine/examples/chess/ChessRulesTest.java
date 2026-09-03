package dev.phlawless.agentengine.examples.chess;

import dev.phlawless.agentengine.game.domain.Command;
import dev.phlawless.agentengine.game.domain.EventSpec;
import dev.phlawless.agentengine.game.domain.RuleResult;
import dev.phlawless.agentengine.game.domain.PlayerContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ChessRulesTest {
    private static final PlayerContext WHITE = new PlayerContext(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), 0);
    private static final PlayerContext BLACK = new PlayerContext(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), 1);

    private final ChessRules rules = new ChessRules();

    @Test
    void whitePawnPushesTwoSquares() {
        ChessState initial = (ChessState) rules.initialState();
        Command move = new Command(ChessRules.MOVE_ACTION, Map.of("from", "e2", "to", "e4"));
        RuleResult result = rules.evaluate(initial, move, WHITE, 0, Instant.now());

        assertThat(result.accepted()).isTrue();
        ChessState next = (ChessState) result.nextState();
        assertThat(next.board().get("e4")).isNotNull();
        assertThat(next.board().get("e4").type()).isEqualTo(PieceType.PAWN);
        assertThat(next.currentPlayer()).isEqualTo(Color.BLACK.marker());
    }

    @Test
    void cannotMoveOpponentsPiece() {
        ChessState initial = (ChessState) rules.initialState();
        Command attempt = new Command(ChessRules.MOVE_ACTION, Map.of("from", "e2", "to", "e3"));
        RuleResult result = rules.evaluate(initial, attempt, BLACK, 0, Instant.now());

        assertThat(result.accepted()).isFalse();
        assertThat(result.message()).contains("own pieces");
    }

    @Test
    void queenMoveResultsInCheckEvent() {
        Map<String, ChessPiece> minimalBoard = new LinkedHashMap<>();
        minimalBoard.put("e1", new ChessPiece(Color.WHITE, PieceType.KING));
        minimalBoard.put("d1", new ChessPiece(Color.WHITE, PieceType.QUEEN));
        minimalBoard.put("e8", new ChessPiece(Color.BLACK, PieceType.KING));
        ChessState custom = new ChessState(minimalBoard, Color.WHITE.marker(), ChessState.IN_PROGRESS, "");

        RuleResult queenMove = rules.evaluate(custom, new Command(ChessRules.MOVE_ACTION, Map.of("from", "d1", "to", "h5")), WHITE, 0, Instant.now());

        assertThat(queenMove.accepted()).isTrue();
        assertThat(queenMove.events()).extracting(EventSpec::type).contains(ChessRules.CHECK_EVENT);
    }
}
