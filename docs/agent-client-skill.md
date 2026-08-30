# Agent Engine Client Skill (Generic)

Use this file as a reusable playbook for an autonomous agent that needs to interact with an Agent Engine server.

This is game-agnostic. It does not assume any specific `GameRules` implementation.

Game-specific skills are strategy modules only. They may interpret the state and
rules for a particular game and select a candidate action, but this client guide
owns authentication, game lifecycle, HTTP requests, polling, retries, and logs.

## Goal

Given a running Agent Engine server, the agent should be able to:

1. authenticate as one or more users,
2. create or join a game,
3. fetch machine-readable rules,
4. choose and submit valid actions,
5. observe state and events until completion.

## Assumptions

- Base URL is available (example: `http://localhost:8080`).
- API base path is `/api/v1`.
- Session auth + CSRF are enabled.
- `jq` is available for JSON extraction.

Optional runtime controls:

- maximum accepted actions or turns before stopping,
- state/event polling interval,
- path for the final event log.

## Required Endpoints

- `GET /api/v1/auth/csrf`
- `POST /api/v1/accounts`
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `POST /api/v1/games`
- `PUT /api/v1/games/{gameId}/players/me`
- `GET /api/v1/games/{gameId}/rules`
- `GET /api/v1/games/{gameId}/state`
- `POST /api/v1/games/{gameId}/actions`
- `GET /api/v1/games/{gameId}/events?afterSequence=N`

## Core Protocol Rules

For every mutating request (`POST`, `PUT`, `DELETE`), send all of:

- session cookie (`JSESSIONID`) via cookie jar,
- CSRF cookie (`XSRF-TOKEN`) via cookie jar,
- CSRF header (`X-XSRF-TOKEN`) with token value.

If CSRF fails, refresh via `GET /api/v1/auth/csrf` and retry once.

## Bootstrapping One User Session

```bash
BASE_URL="http://localhost:8080"
USER="agent_user"
PASS="replace-with-strong-password"
COOKIE_JAR="agent.cookies"

CSRF=$(curl -s -c "$COOKIE_JAR" "$BASE_URL/api/v1/auth/csrf" | jq -r '.token')

curl -s -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  -H "content-type: application/json" \
  -H "X-XSRF-TOKEN: $CSRF" \
  -X POST "$BASE_URL/api/v1/accounts" \
  -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}"

curl -s -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  -H "content-type: application/json" \
  -H "X-XSRF-TOKEN: $CSRF" \
  -X POST "$BASE_URL/api/v1/auth/login" \
  -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}"
```

## Game Setup Flow

### Option A: Create a game

```bash
GAME_ID=$(curl -s -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  -H "X-XSRF-TOKEN: $CSRF" \
  -X POST "$BASE_URL/api/v1/games" | jq -r '.state.gameId')
```

### Option B: Join an existing game

```bash
curl -s -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  -H "X-XSRF-TOKEN: $CSRF" \
  -X PUT "$BASE_URL/api/v1/games/$GAME_ID/players/me"
```

## Agent Decision Loop (Game-Agnostic)

1. Fetch rules once (or when game changes):

```bash
RULES_JSON=$(curl -s -b "$COOKIE_JAR" "$BASE_URL/api/v1/games/$GAME_ID/rules")
```

2. Fetch current state:

```bash
STATE_JSON=$(curl -s -b "$COOKIE_JAR" "$BASE_URL/api/v1/games/$GAME_ID/state")
```

3. Determine candidate action from:

- `rules.actions[]` (action type + payload schema),
- `rules.observableState` (field semantics),
- `state.state` (current observable values),
- `state.turn`, `state.ready`, and participant data.

If a game-specific strategy skill is available, give it the rules, observable
state, and the actor's participant information. It should return a strategic
choice, not perform HTTP requests. The client remains responsible for encoding
that choice as an action allowed by `rules.actions[]`.

4. Build action payload that satisfies schema constraints.

5. Submit action:

```bash
ACTION_TYPE="REPLACE_WITH_RULE_ACTION"
ACTION_PAYLOAD='{}'

RESULT=$(curl -s -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  -H "content-type: application/json" \
  -H "X-XSRF-TOKEN: $CSRF" \
  -X POST "$BASE_URL/api/v1/games/$GAME_ID/actions" \
  -d "{\"type\":\"$ACTION_TYPE\",\"payload\":$ACTION_PAYLOAD}")
```

6. Handle result:

- if `accepted == true`: consume `state` and emitted `events`.
- if `accepted == false`: read `message`, revise plan, try another valid action.

7. Poll events for opponent updates:

```bash
AFTER_SEQ=0
EVENTS=$(curl -s -b "$COOKIE_JAR" "$BASE_URL/api/v1/games/$GAME_ID/events?afterSequence=$AFTER_SEQ")
```

Update `AFTER_SEQ` to the max received `sequence` and continue.

When the game is not ready, it is not the actor's turn, or no action should be
submitted yet, wait for the configured polling interval and fetch fresh state.
Stop on a terminal state described by the game rules or when the configured
budget is exhausted.

## Minimal Agent Policy

- Always call `/rules` before first move.
- Never hardcode action payload shape.
- Validate payload against `rules.actions[*].payload` constraints before submit.
- Treat server as source of truth for legality and state progression.
- On rejection, adapt using returned message and latest state.
- Keep game strategy separate from transport and lifecycle handling.
- Record request/response summaries, action choices, and emitted events.
- At completion, write the collected game events to the configured log path.

## Error Handling

- `401/403`: not authenticated or not authorized for this game.
- `403` on mutating requests may be CSRF-related; refresh token and retry.
- `404`: game does not exist.
- `409`: conflict (for example, join when game is full).
- `400`: invalid request shape or parameters.

## Multi-Agent / Two-User Testing Pattern

- Use one cookie jar per actor (for example `user_a.cookies`, `user_b.cookies`).
- Each actor runs the same loop independently against the same `gameId`.
- Never share cookie jars between actors.

## What to Provide When Prompting an Agent

When you tell an autonomous agent to "go", provide:

1. `BASE_URL`
2. credentials to use (or instruction to register new users)
3. whether to create or join game
4. target `gameId` if joining
5. win condition / objective (if external to rules)

## Suggested Prompt Snippet

"Use `docs/agent-client-skill.md` as your execution guide. Authenticate, join or create a game, fetch `/rules`, and only submit actions that satisfy the returned schema. Continue until terminal game state or max turn budget. Log every request/response summary, and at completion write a detailed log of all game events (including turn ownership and host/opponent designation) to `./logs/{gameId}.json`, creating the directory if needed."
