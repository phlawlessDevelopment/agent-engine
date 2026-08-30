---
name: agent-engine-tictactoe-player
description: Use to choose strong TicTacToe moves from a board position. Triggers: TicTacToe, board strategy, win in one, block two in a row, minimax.
---

# TicTacToe Player Strategy

Use this skill to choose a strong move from a TicTacToe board position.

## Game Model

The board is a length-9 array containing `"X"`, `"O"`, or `""` for an empty
cell. Positions map to the board as follows:

```text
0 | 1 | 2
3 | 4 | 5
6 | 7 | 8
```

Seat `0` plays `X`; seat `1` plays `O`. `X` moves first. The legal moves are
the indices whose board value is empty. A player wins by occupying one of:

```text
[0,1,2] [3,4,5] [6,7,8]
[0,3,6] [1,4,7] [2,5,8]
[0,4,8] [2,4,6]
```

Only choose a move when the game status is `IN_PROGRESS` and `currentPlayer`
matches this player's marker.

## Move Selection Policy

Choose moves in this order:

1. **Win now**: if any legal move gives your marker three in a row, play it.
2. **Block now**: if opponent has an immediate winning move next turn, block it.
3. **Take center**: if position `4` is empty, play `4`.
4. **Solve remaining tree**:
   Evaluate legal moves by minimax from the current board. Maximize the outcome
   for your marker (`win > draw > loss`). Prefer the shortest forced win and,
   when every line loses, the longest resistance.
5. **Stable tie-break** for equal scores:
   Prefer corners in order `0, 2, 6, 8`, then edges in order `1, 3, 5, 7`,
   then numeric order.

This keeps behavior simple but effectively unbeatable.

## Strategy Output

Return the selected board position and a short rationale such as `win now`,
`block O at 6`, `take center`, or `minimax: forced draw`.
