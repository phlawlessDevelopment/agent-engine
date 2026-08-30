# Example Games

This directory contains runnable server examples and optional client skills.

Each server example is a separate Spring Boot module with exactly one `GameRules` bean.

## Modules

- `examples/tictactoe`: two-player TicTacToe server + client skill example
- `examples/wait`: single-player wait/turn-advance server
- `examples/starter`: minimal starter server for new game authors

## Run A Server Example

```bash
./mvnw -pl examples/tictactoe spring-boot:run
```

Swap `tictactoe` for `wait` or `starter`.

## Client Skills

- `examples/tictactoe/clients/agent-engine-tictactoe-player/SKILL.md`

For the game-agnostic interaction protocol, see `docs/agent-client-skill.md`.
