# Chess Example

Runnable two-player server example using `ChessRules`. The ruleset supports legal movement, captures, checks, checkmate, stalemate, and promotions; castling, en passant, and advanced draw claims are intentionally unsupported.

Run:

```bash
./mvnw -pl examples/chess -am spring-boot:run
```

Optional strategy skill (pair with `docs/agent-client-skill.md` for Agent Engine interaction):

- `examples/chess/clients/agent-engine-chess-player/SKILL.md`
