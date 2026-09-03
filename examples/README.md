# Example Games

This directory contains runnable server examples and optional client skills.

Each server example is a separate Spring Boot module with exactly one `GameRules` bean.

## Modules

- `examples/tictactoe`: two-player TicTacToe server + client skill example
- `examples/chess`: core chess server + client skill example

## Run A Server Example

```bash
./mvnw -pl examples/tictactoe -am spring-boot:run
```

```bash
./mvnw -pl examples/chess -am spring-boot:run
```

## Client Skills

- Game-agnostic Agent Engine client protocol: `docs/agent-client-skill.md`
- TicTacToe strategy only: `examples/tictactoe/clients/agent-engine-tictactoe-player/SKILL.md`
- Chess strategy only: `examples/chess/clients/agent-engine-chess-player/SKILL.md`

Compose the client protocol with a game-specific strategy; game skills should
not duplicate authentication, HTTP, polling, recovery, or logging guidance.
