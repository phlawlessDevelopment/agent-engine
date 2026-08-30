package dev.phlawless.agentengine.game.api;

import dev.phlawless.agentengine.game.application.GameService;
import dev.phlawless.agentengine.game.domain.Command;
import dev.phlawless.agentengine.game.domain.GameEvent;
import dev.phlawless.agentengine.game.domain.GameSnapshot;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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
    public CreateGameResponse createGame() {
        GameSnapshot snapshot = gameService.createGame();
        return new CreateGameResponse(toObservableState(snapshot));
    }

    @GetMapping("/{gameId}/state")
    public ObservableStateResponse getState(@PathVariable UUID gameId) {
        GameSnapshot snapshot = gameService.getState(gameId);
        return toObservableState(snapshot);
    }

    @PostMapping("/{gameId}/actions")
    public SubmitActionResponse submitAction(
            @PathVariable UUID gameId,
            @Valid @RequestBody SubmitActionRequest request
    ) {
        GameService.ActionExecutionResult result = gameService.executeAction(
                gameId,
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
            @RequestParam(name = "afterSequence", defaultValue = "0") long afterSequence
    ) {
        return gameService.getEvents(gameId, afterSequence)
                .stream()
                .map(this::toEventResponse)
                .toList();
    }

    private ObservableStateResponse toObservableState(GameSnapshot snapshot) {
        return new ObservableStateResponse(
                snapshot.gameId(),
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
                event.details()
        );
    }
}
