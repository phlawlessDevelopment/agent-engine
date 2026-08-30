# Agent Engine

Agent Engine is a Spring Boot library for authoritative, turn-based game simulation.

You provide one `GameRules` implementation, and the library provides:

- HTTP endpoints (`/api/v1/games`, `/state`, `/actions`, `/events`)
- session lifecycle and server-owned game state
- append-only event sequencing
- validation + ProblemDetail error responses
- default in-memory persistence for quick starts

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

## Minimal consumer application

```java
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

`MyGameRules` must implement `GameRules`:

```java
public class MyGameRules implements GameRules {
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
        return RuleResult.accept(next, List.of(new EventSpec("PLAYED", Map.of("turn", Integer.toString(turn + 1)))));
    }
}
```

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
