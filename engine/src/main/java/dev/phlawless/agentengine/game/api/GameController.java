package dev.phlawless.agentengine.game.api;

import dev.phlawless.agentengine.account.domain.AccountIdentity;
import dev.phlawless.agentengine.game.application.GameService;
import dev.phlawless.agentengine.game.domain.ActionSchema;
import dev.phlawless.agentengine.game.domain.Command;
import dev.phlawless.agentengine.game.domain.EventSchema;
import dev.phlawless.agentengine.game.domain.GameEvent;
import dev.phlawless.agentengine.game.domain.GameParticipant;
import dev.phlawless.agentengine.game.domain.GameRulesDescription;
import dev.phlawless.agentengine.game.domain.GameSnapshot;
import dev.phlawless.agentengine.game.domain.ValueSchema;
import dev.phlawless.agentengine.security.AuthenticatedAccount;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/games")
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateGameResponse createGame(@AuthenticationPrincipal AuthenticatedAccount principal) {
        GameSnapshot snapshot = gameService.createGame(identity(principal));
        return new CreateGameResponse(toObservableState(snapshot));
    }

    @PutMapping("/{gameId}/players/me")
    public JoinGameResponse joinGame(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal AuthenticatedAccount principal
    ) {
        GameSnapshot snapshot = gameService.joinGame(gameId, identity(principal));
        return new JoinGameResponse(toObservableState(snapshot));
    }

    @GetMapping("/{gameId}/state")
    public ObservableStateResponse getState(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal AuthenticatedAccount principal
    ) {
        GameSnapshot snapshot = gameService.getState(gameId, principal.accountId());
        return toObservableState(snapshot);
    }

    @GetMapping("/{gameId}/rules")
    public GameRulesResponse getRules(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal AuthenticatedAccount principal
    ) {
        GameRulesDescription description = gameService.getRules(gameId, principal.accountId());
        return toRulesResponse(description);
    }

    @PostMapping("/{gameId}/actions")
    public SubmitActionResponse submitAction(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal AuthenticatedAccount principal,
            @Valid @RequestBody SubmitActionRequest request
    ) {
        GameService.ActionExecutionResult result = gameService.executeAction(
                gameId,
                principal.accountId(),
                new Command(request.type(), request.payload()));
        return new SubmitActionResponse(
                result.accepted(),
                result.message(),
                toObservableState(result.snapshot()),
                result.emittedEvents().stream().map(this::toEventResponse).toList()
        );
    }

    @GetMapping("/{gameId}/events")
    public List<EventResponse> getEvents(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal AuthenticatedAccount principal,
            @RequestParam(name = "afterSequence", defaultValue = "0") long afterSequence
    ) {
        return gameService.getEvents(gameId, principal.accountId(), afterSequence)
                .stream()
                .map(this::toEventResponse)
                .toList();
    }

    private ObservableStateResponse toObservableState(GameSnapshot snapshot) {
        return new ObservableStateResponse(
                snapshot.gameId(),
                snapshot.requiredPlayerCount(),
                snapshot.ready(),
                snapshot.participants().stream().map(this::toParticipantResponse).toList(),
                snapshot.actionTypes(),
                snapshot.turn(),
                snapshot.state(),
                snapshot.createdAt(),
                snapshot.updatedAt()
        );
    }

    private EventResponse toEventResponse(GameEvent event) {
        return new EventResponse(
                event.sequence(),
                event.turn(),
                event.type(),
                event.occurredAt(),
                event.actorAccountId(),
                event.actorSeat(),
                event.details()
        );
    }

    private GameParticipantResponse toParticipantResponse(GameParticipant participant) {
        return new GameParticipantResponse(participant.accountId(), participant.username(), participant.seat());
    }

    private GameRulesResponse toRulesResponse(GameRulesDescription description) {
        return new GameRulesResponse(
                description.game(),
                description.description(),
                description.requiredPlayerCount(),
                description.actions().stream().map(this::toActionSchemaResponse).toList(),
                toValueSchemaResponses(description.observableState()),
                description.events().stream().map(this::toEventSchemaResponse).toList()
        );
    }

    private ActionSchemaResponse toActionSchemaResponse(ActionSchema schema) {
        return new ActionSchemaResponse(
                schema.type(),
                schema.description(),
                toValueSchemaResponses(schema.payload())
        );
    }

    private EventSchemaResponse toEventSchemaResponse(EventSchema schema) {
        return new EventSchemaResponse(
                schema.type(),
                schema.description(),
                toValueSchemaResponses(schema.details())
        );
    }

    private Map<String, ValueSchemaResponse> toValueSchemaResponses(Map<String, ValueSchema> schemas) {
        return schemas.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> toValueSchemaResponse(entry.getValue()),
                        (left, right) -> right,
                        java.util.LinkedHashMap::new));
    }

    private ValueSchemaResponse toValueSchemaResponse(ValueSchema schema) {
        return new ValueSchemaResponse(schema.type(), schema.required(), schema.description(), schema.constraints());
    }

    private AccountIdentity identity(AuthenticatedAccount principal) {
        return principal.toIdentity();
    }
}
