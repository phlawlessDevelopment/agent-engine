---
name: agent-engine-chess-player
description: Use to select legal chess moves from a board position. Triggers: Chess, checkmate, block, promotion, strong capture.
---

# Chess Player Strategy

Use this skill alongside `docs/agent-client-skill.md` for HTTP, auth, and polling guidance.

## Game Model

The board is a map from algebraic squares (`a1` through `h8`) to either an empty string or piece notation such as `wP`, `bQ`, etc. Seat `0` controls WHITE; seat `1` controls BLACK. White moves first. The available action is `MOVE` with payload:

- `from`: source square
- `to`: destination square
- `promotion`: optional piece type for pawn promotion (`QUEEN`, `ROOK`, `BISHOP`, `KNIGHT`)

Only propose a move when `status` is `IN_PROGRESS` and `currentPlayer` matches your color.

## Move Selection Policy

1. **Checkmate now**: if any legal move produces `GAME_WON` in the event stream, play it.
2. **Prevent checkmate**: if the opponent threatens a checkmate next turn, interpose a block, capture the attacking piece, or move the king.
3. **Capture or check**: prefer captures that win material or moves that deliver `CHECK`.
4. **Maintain structure**: keep pieces defended, avoid dropping back-rank pawns, and prefer center control.
5. **Promotion awareness**: when a pawn reaches the last rank, supply a `promotion` target (default to `QUEEN`).
6. **Fallback safe move**: choose any legal move that keeps the king out of check.

## Strategy Output

Return the selected `from`/`to` pair, optional `promotion`, and a short rationale such as `checkmate`, `block at e8`, or `promote to Q`.
