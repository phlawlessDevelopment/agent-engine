---
name: agent-engine-tictactoe-player
description: Use when an agent must play TicTacToe through Agent Engine HTTP endpoints (`/api/v1/games/*`) with session auth + CSRF. Triggers: TicTacToe, PLACE_MARKER, board, block two in a row, center move.
---

# Agent Engine TicTacToe Player

Use this skill to autonomously play TicTacToe against another participant on an Agent Engine server.

This skill is game-specific and should be paired with the generic engine protocol in `docs/agent-client-skill.md`.

## Inputs You Need

- `BASE_URL` (example: `http://localhost:8080`)
- credentials for this player (`username`, `password`)
- either:
  - `GAME_ID` to join, or
  - instruction to create a game

Optional:

- `MAX_TURNS` safeguard (default `30`)
- `POLL_MS` event/state polling interval (default `800`)

## Protocol

1. Authenticate with session + CSRF (`/auth/csrf`, `/accounts`, `/auth/login`).
2. Join or create a game.
3. Call `/games/{gameId}/rules` once and verify:
   - `game == "TicTacToe"`
   - one action with type `PLACE_MARKER`
   - payload includes integer `position` with min `0` and max `8`
4. On each cycle:
   - read `/games/{gameId}/state`
   - if terminal (`status != IN_PROGRESS`), stop
   - if not ready or not your turn, wait and poll
   - if your turn, choose move and submit action

## Determine Your Marker

1. `GET /api/v1/auth/me` to get your `accountId`.
2. From `state.players`, find matching participant.
3. Seat mapping:
   - seat `0` => marker `X`
   - seat `1` => marker `O`
4. Confirm turn ownership using `state.state.currentPlayer` (`X` or `O`).

## Move Selection Policy

Given `board` as indices:

```text
0 | 1 | 2
3 | 4 | 5
6 | 7 | 8
```

Choose moves in this order:

1. **Win now**: if any legal move gives your marker three in a row, play it.
2. **Block now**: if opponent has an immediate winning move next turn, block it.
3. **Take center**: if position `4` is empty, play `4`.
4. **Solve remaining tree**:
   - evaluate legal moves by game-tree search (minimax) from current board
   - maximize outcome for your marker (`win > draw > loss`)
   - if tied, prefer lowest search depth to win and greatest depth to lose
5. **Stable tie-break** for equal scores:
   - corners first: `0, 2, 6, 8`
   - then edges: `1, 3, 5, 7`
   - then numeric order

This keeps behavior simple but effectively unbeatable.

## Action Submission

Submit exactly:

```json
{
  "type": "PLACE_MARKER",
  "payload": { "position": <0-8> }
}
```

On `accepted=false`, immediately refresh state and recalculate. Do not repeat the same move blindly.

## Terminal Conditions

Stop when any is true:

- `state.state.status == "WINNER"`
- `state.state.status == "DRAW"`
- turn/time budget exceeded

Report final:

- winner marker (`state.state.winner`) or draw
- final board
- number of accepted moves by this player

## Polling Pattern

- Prefer polling `state` directly every `POLL_MS` when waiting for turn.
- Optionally poll `events?afterSequence=N` for richer logs.
- Always refresh `state` before selecting a move.

## Error Recovery

- `403` on mutating calls can be CSRF/session mismatch: fetch `/auth/csrf` again and retry once.
- `409` on join means game is full.
- `404` means game not found.
- Any non-2xx unexpected error: log response body and stop safely.

## Suggested Operator Prompt

Use this when launching an agent:

"Use `examples/tictactoe/clients/agent-engine-tictactoe-player/SKILL.md`. Authenticate with the provided credentials, join game `${GAME_ID}` (or create one if none provided), then play until terminal state. Always follow: win-now, block-now, center, minimax fallback. Log each request and chosen move with rationale."
