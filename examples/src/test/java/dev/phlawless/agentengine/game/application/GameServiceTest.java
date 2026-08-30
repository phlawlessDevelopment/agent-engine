package dev.phlawless.agentengine.game.application;

import dev.phlawless.agentengine.game.domain.Command;
import dev.phlawless.agentengine.game.domain.GameEvent;
import dev.phlawless.agentengine.game.domain.GameRules;
import dev.phlawless.agentengine.game.domain.GameSnapshot;
import dev.phlawless.agentengine.game.infrastructure.InMemoryGameRepository;
import dev.phlawless.agentengine.examples.tictactoe.TicTacToeRules;
import dev.phlawless.agentengine.examples.wait.WaitRules;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameServiceTest {

    @Test
    void createGameUsesConfiguredRulesWithFreshBoard() {
        GameService service = buildService();

        GameSnapshot snapshot = service.createGame();

        assertThat(snapshot.turn()).isZero();
        assertThat(snapshot.actionTypes()).containsExactly("PLACE_MARKER");
        assertThat(snapshot.state().get("board")).asList().hasSize(9).containsOnly("");
        assertThat(snapshot.state().get("currentPlayer")).isEqualTo("X");
        assertThat(snapshot.state().get("status")).isEqualTo("IN_PROGRESS");
    }

    @Test
    void waitActionAdvancesTurnAndEmitsEvent() {
        GameService service = buildService(new WaitRules());
        GameSnapshot created = service.createGame();

        GameService.ActionExecutionResult result = service.executeAction(
                created.gameId(),
                new Command(WaitRules.WAIT_ACTION, Map.of()));

        assertThat(result.accepted()).isTrue();
        assertThat(result.snapshot().turn()).isEqualTo(1);
        assertThat(result.emittedEvents()).hasSize(1);
        assertThat(result.emittedEvents().getFirst().type()).isEqualTo(WaitRules.TURN_ADVANCED_EVENT);
    }

    @Test
    void placingMarkerUpdatesBoardAndEmitsEvent() {
        GameService service = buildService();
        GameSnapshot created = service.createGame();

        GameService.ActionExecutionResult result = service.executeAction(
                created.gameId(),
                new Command(TicTacToeRules.PLACE_MARKER_ACTION, Map.of("position", 0)));

        assertThat(result.accepted()).isTrue();
        assertThat(result.snapshot().turn()).isEqualTo(1);
        assertThat(result.snapshot().state().get("board")).asList().first().isEqualTo("X");
        assertThat(result.snapshot().state().get("currentPlayer")).isEqualTo("O");
        assertThat(result.emittedEvents()).hasSize(1);
        assertThat(result.emittedEvents().getFirst().type()).isEqualTo(TicTacToeRules.MARKER_PLACED_EVENT);
    }

    @Test
    void rejectedCommandChangesNothing() {
        GameService service = buildService();
        GameSnapshot created = service.createGame();

        GameService.ActionExecutionResult result = service.executeAction(
                created.gameId(),
                new Command(TicTacToeRules.PLACE_MARKER_ACTION, Map.of("position", 42)));

        assertThat(result.accepted()).isFalse();
        assertThat(result.snapshot().turn()).isZero();
        assertThat(result.emittedEvents()).isEmpty();
        assertThat(result.snapshot().state().get("board")).asList().containsOnly("");
    }

    @Test
    void unknownActionIsRejected() {
        GameService service = buildService();
        GameSnapshot created = service.createGame();

        GameService.ActionExecutionResult result = service.executeAction(
                created.gameId(),
                new Command("RESET", Map.of()));

        assertThat(result.accepted()).isFalse();
        assertThat(result.snapshot().turn()).isZero();
    }

    @Test
    void cellCannotBeOccupiedTwice() {
        GameService service = buildService();
        GameSnapshot created = service.createGame();

        service.executeAction(created.gameId(), new Command(TicTacToeRules.PLACE_MARKER_ACTION, Map.of("position", 0)));

        GameService.ActionExecutionResult result = service.executeAction(
                created.gameId(),
                new Command(TicTacToeRules.PLACE_MARKER_ACTION, Map.of("position", 0)));

        assertThat(result.accepted()).isFalse();
        assertThat(result.snapshot().state().get("board")).asList().first().isEqualTo("X");
    }

    @Test
    void threeInARowEndsGameWithWinner() {
        GameService service = buildService();
        GameSnapshot created = service.createGame();

        service.executeAction(created.gameId(), marker(0));
        service.executeAction(created.gameId(), marker(3));
        service.executeAction(created.gameId(), marker(1));
        service.executeAction(created.gameId(), marker(4));
        GameService.ActionExecutionResult result = service.executeAction(created.gameId(), marker(2));

        assertThat(result.accepted()).isTrue();
        assertThat(result.snapshot().state().get("status")).isEqualTo("WINNER");
        assertThat(result.snapshot().state().get("winner")).isEqualTo("X");
        assertThat(result.emittedEvents())
                .map(GameEvent::type)
                .containsExactly("MARKER_PLACED", "GAME_WON");

        GameService.ActionExecutionResult after = service.executeAction(
                created.gameId(),
                new Command(TicTacToeRules.PLACE_MARKER_ACTION, Map.of("position", 6)));

        assertThat(after.accepted()).isFalse();
    }

    @Test
    void fullBoardWithoutWinnerIsADraw() {
        GameService service = buildService();
        GameSnapshot created = service.createGame();

        service.executeAction(created.gameId(), marker(0));
        service.executeAction(created.gameId(), marker(1));
        service.executeAction(created.gameId(), marker(2));
        service.executeAction(created.gameId(), marker(4));
        service.executeAction(created.gameId(), marker(3));
        service.executeAction(created.gameId(), marker(5));
        service.executeAction(created.gameId(), marker(7));
        service.executeAction(created.gameId(), marker(6));
        GameService.ActionExecutionResult result = service.executeAction(created.gameId(), marker(8));

        assertThat(result.accepted()).isTrue();
        assertThat(result.snapshot().state().get("status")).isEqualTo("DRAW");
        assertThat(result.emittedEvents())
                .map(GameEvent::type)
                .containsExactly("MARKER_PLACED", "GAME_DRAWN");
    }

    @Test
    void eventsAreAppendOnlyAndSequenced() {
        GameService service = buildService();
        GameSnapshot created = service.createGame();

        service.executeAction(created.gameId(), marker(0));
        service.executeAction(created.gameId(), marker(1));

        List<GameEvent> events = service.getEvents(created.gameId(), 0);

        assertThat(events).hasSize(3);
        assertThat(events).extracting(GameEvent::sequence).containsExactly(1L, 2L, 3L);
        assertThat(events).extracting(GameEvent::type)
                .containsExactly("GAME_CREATED", "MARKER_PLACED", "MARKER_PLACED");
    }

    @Test
    void eventsAfterSequenceReturnsOnlyNewEvents() {
        GameService service = buildService();
        GameSnapshot created = service.createGame();

        service.executeAction(created.gameId(), marker(0));

        List<GameEvent> events = service.getEvents(created.gameId(), 1);

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().sequence()).isEqualTo(2);
    }

    @Test
    void unknownGameThrowsNotFoundException() {
        GameService service = buildService();

        assertThatThrownBy(() -> service.getEvents(UUID.randomUUID(), 0))
                .isInstanceOf(GameNotFoundException.class);
    }

    private Command marker(int position) {
        return new Command(TicTacToeRules.PLACE_MARKER_ACTION, Map.of("position", position));
    }

    private GameService buildService() {
        return buildService(new TicTacToeRules());
    }

    private GameService buildService(GameRules rules) {
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T12:00:00Z"), ZoneOffset.UTC);
        return new GameService(new InMemoryGameRepository(), rules, clock);
    }
}
