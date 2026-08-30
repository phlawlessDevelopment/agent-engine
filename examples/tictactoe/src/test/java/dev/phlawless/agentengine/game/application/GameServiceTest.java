package dev.phlawless.agentengine.game.application;

import dev.phlawless.agentengine.account.domain.AccountIdentity;
import dev.phlawless.agentengine.examples.tictactoe.TicTacToeRules;
import dev.phlawless.agentengine.game.domain.Command;
import dev.phlawless.agentengine.game.domain.GameEvent;
import dev.phlawless.agentengine.game.domain.GameRules;
import dev.phlawless.agentengine.game.domain.GameRulesDescription;
import dev.phlawless.agentengine.game.domain.GameSnapshot;
import dev.phlawless.agentengine.game.infrastructure.InMemoryGameRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameServiceTest {
    private static final AccountIdentity ALICE = new AccountIdentity(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), "alice");
    private static final AccountIdentity BOB = new AccountIdentity(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), "bob");
    private static final AccountIdentity CAROL = new AccountIdentity(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"), "carol");

    @Test
    void createGameAssignsCreatorToSeatZero() {
        GameService service = buildService();

        GameSnapshot snapshot = service.createGame(ALICE);

        assertThat(snapshot.requiredPlayerCount()).isEqualTo(2);
        assertThat(snapshot.ready()).isFalse();
        assertThat(snapshot.participants()).hasSize(1);
        assertThat(snapshot.participants().getFirst().accountId()).isEqualTo(ALICE.accountId());
        assertThat(snapshot.participants().getFirst().seat()).isZero();
    }

    @Test
    void joinGameAddsSecondSeatAndIsIdempotent() {
        GameService service = buildService();
        GameSnapshot created = service.createGame(ALICE);

        GameSnapshot joined = service.joinGame(created.gameId(), BOB);
        GameSnapshot rejoined = service.joinGame(created.gameId(), BOB);

        assertThat(joined.ready()).isTrue();
        assertThat(joined.participants()).hasSize(2);
        assertThat(joined.participants().get(1).accountId()).isEqualTo(BOB.accountId());
        assertThat(joined.participants().get(1).seat()).isEqualTo(1);
        assertThat(rejoined.participants()).hasSize(2);
    }

    @Test
    void gameFullRejectsThirdJoin() {
        GameService service = buildService();
        GameSnapshot created = service.createGame(ALICE);
        service.joinGame(created.gameId(), BOB);

        assertThatThrownBy(() -> service.joinGame(created.gameId(), CAROL))
                .isInstanceOf(GameFullException.class);
    }

    @Test
    void nonParticipantCannotReadStateOrEvents() {
        GameService service = buildService();
        GameSnapshot created = service.createGame(ALICE);
        service.joinGame(created.gameId(), BOB);

        assertThatThrownBy(() -> service.getState(created.gameId(), CAROL.accountId()))
                .isInstanceOf(NotGameParticipantException.class);
        assertThatThrownBy(() -> service.getEvents(created.gameId(), CAROL.accountId(), 0))
                .isInstanceOf(NotGameParticipantException.class);
        assertThatThrownBy(() -> service.getRules(created.gameId(), CAROL.accountId()))
                .isInstanceOf(NotGameParticipantException.class);
    }

    @Test
    void participantCanReadRulesDescription() {
        GameService service = buildService();
        GameSnapshot created = service.createGame(ALICE);
        service.joinGame(created.gameId(), BOB);

        GameRulesDescription rules = service.getRules(created.gameId(), ALICE.accountId());

        assertThat(rules.game()).isEqualTo("TicTacToe");
        assertThat(rules.actions()).extracting(action -> action.type())
                .containsExactly(TicTacToeRules.PLACE_MARKER_ACTION);
        assertThat(rules.actions().getFirst().payload()).containsKey("position");
        assertThat(rules.observableState()).containsKeys("board", "currentPlayer", "status", "winner");
    }

    @Test
    void cannotExecuteActionUntilGameReady() {
        GameService service = buildService();
        GameSnapshot created = service.createGame(ALICE);

        assertThatThrownBy(() -> service.executeAction(created.gameId(), ALICE.accountId(), marker(0)))
                .isInstanceOf(GameNotReadyException.class);
    }

    @Test
    void seatsControlTurnOrder() {
        GameService service = buildService();
        GameSnapshot created = service.createGame(ALICE);
        service.joinGame(created.gameId(), BOB);

        GameService.ActionExecutionResult bobFirst = service.executeAction(created.gameId(), BOB.accountId(), marker(0));
        assertThat(bobFirst.accepted()).isFalse();
        assertThat(bobFirst.message()).isEqualTo("It is not your turn");

        GameService.ActionExecutionResult aliceFirst = service.executeAction(created.gameId(), ALICE.accountId(), marker(0));
        assertThat(aliceFirst.accepted()).isTrue();
        assertThat(aliceFirst.snapshot().state().get("currentPlayer")).isEqualTo("O");
    }

    @Test
    void actionEventsCarryActorIdentityAndSeat() {
        GameService service = buildService();
        GameSnapshot created = service.createGame(ALICE);
        service.joinGame(created.gameId(), BOB);

        GameService.ActionExecutionResult result = service.executeAction(created.gameId(), ALICE.accountId(), marker(0));

        assertThat(result.accepted()).isTrue();
        GameEvent actionEvent = result.emittedEvents().getFirst();
        assertThat(actionEvent.actorAccountId()).isEqualTo(ALICE.accountId());
        assertThat(actionEvent.actorSeat()).isZero();
    }

    @Test
    void unknownGameThrowsNotFoundException() {
        GameService service = buildService();

        assertThatThrownBy(() -> service.getEvents(UUID.randomUUID(), ALICE.accountId(), 0))
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
