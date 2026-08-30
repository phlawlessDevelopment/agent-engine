# Example Games

This directory contains runnable server examples and optional client skills.

Each server example is a separate Spring Boot module with exactly one `GameRules` bean.

## Modules

- `examples/tictactoe`: two-player TicTacToe server + client skill example
- `examples/wait`: single-player wait/turn-advance server
- `examples/starter`: minimal starter server for new game authors

## Run A Server Example

```bash
./mvnw -pl examples/tictactoe -am spring-boot:run
```

Swap `tictactoe` for `wait` or `starter`.

## Client Skills

- Game-agnostic Agent Engine client protocol: `docs/agent-client-skill.md`
- TicTacToe strategy only: `examples/tictactoe/clients/agent-engine-tictactoe-player/SKILL.md`

Compose the client protocol with a game-specific strategy; game skills should
not duplicate authentication, HTTP, polling, recovery, or logging guidance.
