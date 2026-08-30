# Agent Engine

An authoritative, agent-native game engine framework for turn-based games.

This project gives you a reusable HTTP engine (`engine` module) and runnable
reference implementations (`examples` module). You bring game rules; the engine
handles game lifecycle, state transitions, event ordering, and API shape.

## Why this exists

- Server-owned state keeps simulation authoritative and consistent.
- Pluggable rules let each game define its own commands and transitions.
- Append-only events make game progression observable for clients and agents.
- Uniform API means agents can switch game types without transport changes.

## Repository layout

| Module | Purpose |
| --- | --- |
| `engine` | Reusable framework (domain, service layer, API controllers, registry, in-memory store) |
| `examples` | Runnable Spring Boot app plus sample games (`wait`, `tictactoe`, `starter`) |

Reference examples live under:

- `examples/src/main/java/dev/phlawless/agentengine/examples/tictactoe`
- `examples/src/main/java/dev/phlawless/agentengine/examples/wait`
- `examples/src/main/java/dev/phlawless/agentengine/examples/starter`

## Quick start

Requirements:

- JDK 27

Run the example application:

```bash
./mvnw -pl examples spring-boot:run
```

Run all tests:

```bash
./mvnw clean verify
```

## API overview

Base path: `/api/v1`

- `POST /games` creates a game (`{"gameType":"tictactoe"}` optional).
- `GET /games/{gameId}/state` returns current observable state.
- `POST /games/{gameId}/actions` submits a command.
- `GET /games/{gameId}/events?afterSequence=0` returns append-only events.

Example flow:

```bash
# Create a game
curl -s -X POST http://localhost:8080/api/v1/games \
  -H 'content-type: application/json' \
  -d '{"gameType":"tictactoe"}'

# Submit an action
curl -s -X POST http://localhost:8080/api/v1/games/<gameId>/actions \
  -H 'content-type: application/json' \
  -d '{"type":"PLACE_MARKER","payload":{"position":0}}'

# Read events since sequence 0
curl -s "http://localhost:8080/api/v1/games/<gameId>/events?afterSequence=0"
```

Observable state shape:

```json
{
  "gameId": "f4f791dd-4be8-4b2f-b86a-70bc6f8d4a96",
  "gameType": "tictactoe",
  "actions": ["PLACE_MARKER"],
  "turn": 0,
  "state": {
    "board": ["", "", "", "", "", "", "", "", ""],
    "currentPlayer": "X",
    "status": "IN_PROGRESS",
    "winner": ""
  },
  "createdAt": "2026-08-30T12:00:00Z",
  "updatedAt": "2026-08-30T12:00:00Z"
}
```

## Build your own game module

Implement the `GameRules` SPI:

1. Provide `gameType()` and `actionTypes()`.
2. Return a fresh state from `initialState()`.
3. Validate and apply commands in `evaluate(...)`.
4. Emit events with `EventSpec` and return `RuleResult`.
5. Add `@Component` so Spring auto-registers your rules bean.

Minimal sketch:

```java
@Component
public class MyGameRules implements GameRules {
    @Override
    public String gameType() {
        return "mygame";
    }

    @Override
    public Set<String> actionTypes() {
        return Set.of("PLAY");
    }

    @Override
    public GameState initialState() {
        return MyGameState.fresh();
    }

    @Override
    public RuleResult evaluate(GameState state, Command command, int turn, Instant now) {
        if (!"PLAY".equals(command.type())) {
            return RuleResult.reject("Unknown action: " + command.type());
        }

        MyGameState next = ((MyGameState) state).apply(command);
        EventSpec event = new EventSpec("PLAYED", Map.of("turn", Integer.toString(turn + 1)));
        return RuleResult.accept(next, List.of(event));
    }
}
```

For a copy/paste starter, use:

- `examples/src/main/java/dev/phlawless/agentengine/examples/starter/StarterGameRules.java`
- `examples/src/main/java/dev/phlawless/agentengine/examples/starter/StarterGameState.java`

Then set your default game type in `application.properties`:

```properties
agent-engine.default-game-type=mygame
```

## Architecture and roadmap

- Architecture notes: `docs/architecture.md`
- Project roadmap: `TODO.md`
