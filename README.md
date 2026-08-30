# Agent Engine

Agent Engine is a Spring Boot library for authoritative, turn-based multiplayer game simulation.

You provide exactly one `GameRules` bean, and the library provides:

- account registration + session login
- CSRF-protected HTTP API
- game lifecycle (`create`, `join`, `state`, `actions`, `events`)
- server-owned state transitions with append-only events
- ProblemDetail error responses
- default in-memory repositories for quick starts

## Project layout

| Module | Purpose |
| --- | --- |
| `engine` | Reusable library consumed by other Spring Boot apps |
| `examples` | Aggregator for runnable server examples + client skills |

Example rule implementations:

- `examples/tictactoe/src/main/java/dev/phlawless/agentengine/examples/tictactoe`
- `examples/wait/src/main/java/dev/phlawless/agentengine/examples/wait`
- `examples/starter/src/main/java/dev/phlawless/agentengine/examples/starter`

Client-side agent examples:

- `examples/README.md`
- `examples/tictactoe/clients/agent-engine-tictactoe-player/SKILL.md`

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

`toObservable()` defines game-specific JSON returned by `GET /api/v1/games/{gameId}/state`.

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
import dev.phlawless.agentengine.game.domain.PlayerContext;
import dev.phlawless.agentengine.game.domain.RuleResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MyGameRules implements GameRules {
    public static final String PLAY_ACTION = "PLAY";
    public static final String PLAYED_EVENT = "PLAYED";

    @Override
    public int requiredPlayerCount() {
        return 2;
    }

    @Override
    public Set<String> actionTypes() {
        return Set.of(PLAY_ACTION);
    }

    @Override
    public GameState initialState() {
        return MyGameState.fresh();
    }

    @Override
    public RuleResult evaluate(GameState state, Command command, PlayerContext player, int turn, Instant now) {
        if (!(state instanceof MyGameState myState)) {
            return RuleResult.reject("Invalid state for my game");
        }
        if (!PLAY_ACTION.equals(command.type())) {
            return RuleResult.reject("Unknown action: " + command.type());
        }

        MyGameState next = myState.applyPlay();
        EventSpec event = new EventSpec(
                PLAYED_EVENT,
                Map.of("seat", Integer.toString(player.seat()), "turn", Integer.toString(turn + 1)));
        return RuleResult.accept(next, List.of(event));
    }
}
```

### 4) Run

```bash
./mvnw spring-boot:run
```

## Two-player API quickstart (register/login/create/join/play)

Base path: `/api/v1`

This API uses session auth + CSRF. For every mutating request (`POST`, `PUT`, `DELETE`), send:

- session cookie (`JSESSIONID`)
- CSRF cookie (`XSRF-TOKEN`)
- CSRF header (default `X-XSRF-TOKEN`)

Example flow with two users:

```bash
# terminal vars
BASE_URL="http://localhost:8080"

# 1) Get CSRF for alice session
ALICE_CSRF=$(curl -s -c alice.cookies "$BASE_URL/api/v1/auth/csrf" | jq -r '.token')

# 2) Register alice
curl -s -b alice.cookies -c alice.cookies \
  -H "content-type: application/json" \
  -H "X-XSRF-TOKEN: $ALICE_CSRF" \
  -X POST "$BASE_URL/api/v1/accounts" \
  -d '{"username":"alice","password":"alice-password-123"}'

# 3) Login alice
curl -s -b alice.cookies -c alice.cookies \
  -H "content-type: application/json" \
  -H "X-XSRF-TOKEN: $ALICE_CSRF" \
  -X POST "$BASE_URL/api/v1/auth/login" \
  -d '{"username":"alice","password":"alice-password-123"}'

# 4) Get CSRF for bob session
BOB_CSRF=$(curl -s -c bob.cookies "$BASE_URL/api/v1/auth/csrf" | jq -r '.token')

# 5) Register bob
curl -s -b bob.cookies -c bob.cookies \
  -H "content-type: application/json" \
  -H "X-XSRF-TOKEN: $BOB_CSRF" \
  -X POST "$BASE_URL/api/v1/accounts" \
  -d '{"username":"bob","password":"bob-password-123"}'

# 6) Login bob
curl -s -b bob.cookies -c bob.cookies \
  -H "content-type: application/json" \
  -H "X-XSRF-TOKEN: $BOB_CSRF" \
  -X POST "$BASE_URL/api/v1/auth/login" \
  -d '{"username":"bob","password":"bob-password-123"}'

# 7) Alice creates game
GAME_ID=$(curl -s -b alice.cookies -c alice.cookies \
  -H "X-XSRF-TOKEN: $ALICE_CSRF" \
  -X POST "$BASE_URL/api/v1/games" | jq -r '.state.gameId')

# 8) Bob joins
curl -s -b bob.cookies -c bob.cookies \
  -H "X-XSRF-TOKEN: $BOB_CSRF" \
  -X PUT "$BASE_URL/api/v1/games/$GAME_ID/players/me"

# 9) Alice (or any participant) fetches machine-readable rules
curl -s -b alice.cookies "$BASE_URL/api/v1/games/$GAME_ID/rules"

# 10) Alice plays
curl -s -b alice.cookies -c alice.cookies \
  -H "content-type: application/json" \
  -H "X-XSRF-TOKEN: $ALICE_CSRF" \
  -X POST "$BASE_URL/api/v1/games/$GAME_ID/actions" \
  -d '{"type":"PLACE_MARKER","payload":{"position":0}}'

# 11) Bob reads state/events
curl -s -b bob.cookies "$BASE_URL/api/v1/games/$GAME_ID/state"
curl -s -b bob.cookies "$BASE_URL/api/v1/games/$GAME_ID/events?afterSequence=0"
```

## Rules contract reference

- `requiredPlayerCount()` defines when a game is ready.
- `Command` shape is `type: String` and `payload: Map<String, Object>`.
- `PlayerContext` contains actor identity and seat.
- `EventSpec` shape is `type: String` and `details: Map<String, String>`.
- `describe()` returns structured JSON metadata for agentic clients at `GET /api/v1/games/{gameId}/rules`.
- `RuleResult.reject(...)` means no state change, no event emission, no turn increment.
- `RuleResult.accept(...)` applies new state, emits events, and increments turn by 1.
- `actionTypes()` is exposed to clients in state responses as available actions.

## Auto-configuration behavior

`agent-engine` auto-registers API and service layers. You do not need package scanning for `dev.phlawless.agentengine`.

Defaults provided by the library:

- `GameController`
- `AccountController`
- `AuthenticationController`
- `RestExceptionHandler`
- `GameService`
- `AccountService`
- `Clock` (`Clock.systemUTC()`)
- `GameRepository` (`InMemoryGameRepository`)
- `AccountRepository` (`InMemoryAccountRepository`)
- Spring Security filter chain with session auth + CSRF enabled

You must provide exactly one `GameRules` bean.

- zero `GameRules` beans -> startup fails
- more than one `GameRules` bean -> startup fails

## Overriding defaults

You can replace defaults with your own beans.

Custom game repository example:

```java
@Bean
GameRepository gameRepository() {
    return new PostgresGameRepository();
}
```

Custom account repository example:

```java
@Bean
AccountRepository accountRepository() {
    return new PostgresAccountRepository();
}
```

Custom clock example:

```java
@Bean
Clock gameClock() {
    return Clock.systemUTC();
}
```

## Run the reference app

```bash
./mvnw -pl examples/tictactoe spring-boot:run
```

## Verify

```bash
./mvnw clean verify
```

## More docs

- Architecture: `docs/architecture.md`
- Roadmap: `TODO.md`
