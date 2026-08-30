# Client Examples

This directory contains client-side examples for agents that play games hosted by Agent Engine.

Server-side game rules live in `examples/src/main/java/dev/phlawless/agentengine/examples/*`.
Client-side examples live here, grouped by game.

## Layout

- `tictactoe/agent-engine-tictactoe-player/SKILL.md`: OpenCode-compatible skill for a TicTacToe player agent.

## Prerequisites

- A running Agent Engine server (for this repo: `./mvnw -pl examples spring-boot:run`)
- `jq`
- A coding agent runtime that supports skills via `SKILL.md`

## Install A Skill Locally (OpenCode)

From the repository root:

```bash
mkdir -p .opencode/skills
cp -R examples/clients/tictactoe/agent-engine-tictactoe-player .opencode/skills/
```

Then restart OpenCode so it reloads skills.

## Generic Protocol Reference

For the game-agnostic API workflow (sessions, CSRF, create/join/state/actions/events), see:

- `docs/agent-client-skill.md`

The game-specific skills in this directory build on that protocol.
