# Architecture

## Layering

Repository modules:

- `engine`: framework contracts + orchestration
- `examples`: runnable app + sample `GameRules` implementations

- `game.api`: HTTP transport, request validation, response mapping
- `game.application`: orchestration and use cases (`GameService`), rules registry contract
- `game.domain`: authoritative simulation state, event emission, the `GameRules` SPI
- `game.infrastructure`: in-memory repository, configured rules registry
- `examples.*`: game-specific rule modules (`wait`, `tictactoe`) — each is a `GameRules` bean

## Core principles

- Server is authoritative.
- Commands are requests, not direct mutations.
- Observable state is a projection of internal state.
- Events are append-only facts that clients can consume.
- The engine is generic; all game-specific logic lives in a `GameRules` module.

## The GameRules SPI

A rules module declares its `gameType`, its action vocabulary, how to construct
fresh state, and how to evaluate a command:

```
GameRules.evaluate(state, command, turn, now) -> RuleResult(accepted, message, nextState, events)
```

The engine owns session lifecycle, event sequencing, turn advancement (only for
accepted actions), and concurrency. It treats `GameState` opaquely and only
ever reads it through `toObservable()` for the projection.

Modules are Spring beans collected into `ConfiguredGameRulesRegistry`; `gameType`
strings must be unique. Unknown game types fail `POST /games` with a 404 problem.

## Events

- `GAME_CREATED` — emitted by the engine for every session
- Game-specific events (e.g. `MARKER_PLACED`, `GAME_WON`, `GAME_DRAWN`,
  `TURN_ADVANCED`) — emitted by rules modules; the engine stamps them with the
  sequence and current turn

Rejected commands emit nothing and leave state untouched; the response carries
`accepted:false` and a reason.
