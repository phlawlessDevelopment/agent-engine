# Agent Engine

Agent Engine is a Spring Boot library for authoritative, turn-based game simulation.

You provide one `GameRules` implementation, and the library provides:

- HTTP endpoints (`/api/v1/games`, `/state`, `/actions`, `/events`)
- session lifecycle and server-owned game state
- append-only event sequencing
- validation + ProblemDetail error responses
- default in-memory persistence for quick starts

The fastest way to learn the contract is the starter example:

- `examples/src/main/java/dev/phlawless/agentengine/examples/starter/StarterGameRules.java`
- `examples/src/main/java/dev/phlawless/agentengine/examples/starter/StarterGameState.java`

## Project layout

| Module | Purpose |
| --- | --- |
| `engine` | Reusable library consumed by other Spring Boot apps |
| `examples` | Runnable sample app that consumes `engine` |

Example rule implementations:

- `examples/src/main/java/dev/phlawless/agentengine/examples/tictactoe`
- `examples/src/main/java/dev/phlawless/agentengine/examples/wait`
- `examples/src/main/java/dev/phlawless/agentengine/examples/starter`

## Add to your project

Until a Maven Central release is published, install locally first:

```bash
git clone https://github.com/phlawlessDevelopment/agent-engine.git
cd agent-engine
./mvnw install
```

Then add the dependency in your own Spring Boot project:

```xml
<dependency>
    <groupId>dev.phlawless</groupId>
    <artifactId>agent-engine</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Build a game in 10 minutes

### 1) Create a Spring Boot app and register one `GameRules` bean

```java
import dev.phlawless.agentengine.game.domain.GameRules;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MyGameApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyGameApplication.class, args);
    }

    @Bean
    GameRules gameRules() {
        return new MyGameRules();
    }
}
```

### 2) Implement your `GameState`

`toObservable()` defines the game-specific JSON shown in `GET /games/{gameId}/state`.

```java
import dev.phlawless.agentengine.game.domain.GameState;

import java.util.Map;

public record MyGameState(int moveCount, String status) implements GameState {
    public static final String IN_PROGRESS = "IN_PROGRESS";

    public static MyGameState fresh() {
        return new MyGameState(0, IN_PROGRESS);
    }

    public MyGameState applyPlay() {
        return new MyGameState(moveCount + 1, IN_PROGRESS);
    }

    @Override
    public Map<String, Object> toObservable() {
        return Map.of(
                "moveCount", moveCount,
                "status", status);
    }
}
```

### 3) Implement your `GameRules`

```java
import dev.phlawless.agentengine.game.domain.Command;
import dev.phlawless.agentengine.game.domain.EventSpec;
import dev.phlawless.agentengine.game.domain.GameRules;
import dev.phlawless.agentengine.game.domain.GameState;
import dev.phlawless.agentengine.game.domain.RuleResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MyGameRules implements GameRules {
    public static final String PLAY_ACTION = "PLAY";
    public static final String PLAYED_EVENT = "PLAYED";

    @Override
    public Set<String> actionTypes() {
        return Set.of(PLAY_ACTION);
    }

    @Override
    public GameState initialState() {
        return MyGameState.fresh();
    }

    @Override
    public RuleResult evaluate(GameState state, Command command, int turn, Instant now) {
        if (!(state instanceof MyGameState myState)) {
            return RuleResult.reject("Invalid state for my game");
        }
        if (!PLAY_ACTION.equals(command.type())) {
            return RuleResult.reject("Unknown action: " + command.type());
        }

        MyGameState next = myState.applyPlay();
        EventSpec event = new EventSpec(
                PLAYED_EVENT,
                Map.of("turn", Integer.toString(turn + 1)));
        return RuleResult.accept(next, List.of(event));
    }
}
```

### 4) Run and call the API

```bash
./mvnw spring-boot:run
```

```bash
# Create a game
curl -s -X POST http://localhost:8080/api/v1/games

# Submit an action
curl -s -X POST http://localhost:8080/api/v1/games/<gameId>/actions \
  -H 'content-type: application/json' \
  -d '{"type":"PLAY","payload":{}}'
```

## Rules contract reference

- `Command` shape is `type: String` and `payload: Map<String, Object>`.
- `EventSpec` shape is `type: String` and `details: Map<String, String>`.
- `RuleResult.reject(...)` means no state change, no event emission, and no turn increment.
- `RuleResult.accept(...)` applies the new state, emits events, and increments turn by 1.
- `actionTypes()` is exposed to clients in state responses as available actions.

## Auto-configuration behavior

`agent-engine` auto-registers the API and service layer. You do **not** need to scan `dev.phlawless.agentengine`.

Defaults provided by the library:

- `GameController`
- `RestExceptionHandler`
- `GameService`
- `Clock` (`Clock.systemUTC()`)
- `GameRepository` (`InMemoryGameRepository`)

You must provide exactly one `GameRules` bean.

- zero `GameRules` beans -> startup fails
- more than one `GameRules` bean -> startup fails

## Overriding defaults

You can replace defaults with your own beans.

Custom repository example:

```java
@Bean
GameRepository gameRepository() {
    return new PostgresGameRepository();
}
```

Custom clock example:

```java
@Bean
Clock gameClock() {
    return Clock.systemUTC();
}
```

## API quick example

Base path: `/api/v1`

```bash
# Create a game
curl -s -X POST http://localhost:8080/api/v1/games

# Submit an action
curl -s -X POST http://localhost:8080/api/v1/games/<gameId>/actions \
  -H 'content-type: application/json' \
  -d '{"type":"PLACE_MARKER","payload":{"position":0}}'

# Read events after sequence 0
curl -s "http://localhost:8080/api/v1/games/<gameId>/events?afterSequence=0"
```

## Run the reference app

```bash
./mvnw -pl examples spring-boot:run
```

## Verify

```bash
./mvnw clean verify
```

## More docs

- Architecture: `docs/architecture.md`
- Roadmap: `TODO.md`
