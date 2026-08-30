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

- Runtime values are provided in a text file (example:
  `docs/agent-client-runtime.txt`).
- API base path is `/api/v1`.
- Session auth + CSRF are enabled.
- `jq` is available for JSON extraction.

Optional runtime controls:

- maximum accepted actions or turns before stopping,
- state/event polling interval,
- path for the final event log.

## Runtime Context File

Before starting, read a plain-text runtime file and export its values as shell
variables. Use this repo template:

- `docs/agent-client-runtime.txt`

Expected keys:

- `BASE_URL`
- `MODE` (`host` or `join`)
- `GAME_ID` (required when `MODE=join`)
- `USER`
- `PASS`
- `COOKIE_JAR`
- `POLL_INTERVAL_MS` (optional)
- `MAX_TURNS` (optional)
- `EVENT_LOG_PATH` (optional)

If a key is missing, apply safe defaults from the template. Do not assume the
game ID unless it is explicitly provided for join mode.

## Required Endpoints

This is the complete client API needed to play a game. Do not inspect server
source code or probe unlisted routes to infer the workflow.

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

Use the same cookie jar for every request made by one actor. Every curl command,
including the CSRF request, must both load and save that jar:

```bash
-b "$COOKIE_JAR" -c "$COOKIE_JAR"
```

Using `-c` without `-b` is destructive after login: curl does not send the
authenticated `JSESSIONID`, then rewrites the jar without it. The next protected
request receives an empty `403` response.

For every mutating request (`POST`, `PUT`, `DELETE`), send all of:

- session cookie (`JSESSIONID`) via cookie jar,
- CSRF cookie (`XSRF-TOKEN`) via cookie jar,
- CSRF header (`X-XSRF-TOKEN`) with the `.token` value returned by
  `GET /api/v1/auth/csrf`.

The JSON token may differ from the `XSRF-TOKEN` cookie because Spring Security
masks the value. Use the JSON `.token` in the header; do not read the header
value from the cookie jar.

Shell variables do not persist between agent tool calls. Re-read the runtime
file and re-declare at least `BASE_URL`, `COOKIE_JAR`, and `GAME_ID` (if joining)
when starting a new shell command. Never delete or replace the cookie jar after
login.

## Bootstrapping One User Session

Run this to register a new account and log in. If the account already exists,
skip the registration request. Usernames must be 3-50 characters using letters,
digits, `.`, `_`, or `-`; passwords must be 12-72 characters.

```bash
set -euo pipefail

BASE_URL="http://localhost:8080"
USER="agent_user"
PASS="replace-with-strong-password"
COOKIE_JAR="./agent.cookies"
touch "$COOKIE_JAR"

CREDENTIALS=$(jq -nc \
  --arg username "$USER" \
  --arg password "$PASS" \
  '{username: $username, password: $password}')

CSRF=$(curl --fail-with-body --silent --show-error \
  -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  "$BASE_URL/api/v1/auth/csrf" | jq -er '.token')

curl --fail-with-body --silent --show-error \
  -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  -H "content-type: application/json" \
  -H "X-XSRF-TOKEN: $CSRF" \
  -X POST "$BASE_URL/api/v1/accounts" \
  -d "$CREDENTIALS"

CSRF=$(curl --fail-with-body --silent --show-error \
  -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  "$BASE_URL/api/v1/auth/csrf" | jq -er '.token')

curl --fail-with-body --silent --show-error \
  -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  -H "content-type: application/json" \
  -H "X-XSRF-TOKEN: $CSRF" \
  -X POST "$BASE_URL/api/v1/auth/login" \
  -d "$CREDENTIALS"

curl --fail-with-body --silent --show-error \
  -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  "$BASE_URL/api/v1/auth/me" | jq -e .
```

Do not continue unless `/auth/me` returns this actor's `accountId` and
`username`.

## Game Setup Flow

### Option A: Create a game

The creator is automatically added as seat `0`; do not call the join endpoint
for the host. This endpoint returns `201` with the game ID at `.state.gameId`.

```bash
set -euo pipefail

BASE_URL="http://localhost:8080"
COOKIE_JAR="./agent.cookies"
touch "$COOKIE_JAR"

curl --fail-with-body --silent --show-error \
  -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  "$BASE_URL/api/v1/auth/me" | jq -e . >/dev/null

CSRF=$(curl --fail-with-body --silent --show-error \
  -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  "$BASE_URL/api/v1/auth/csrf" | jq -er '.token')

GAME_JSON=$(curl --fail-with-body --silent --show-error \
  -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  -H "X-XSRF-TOKEN: $CSRF" \
  -X POST "$BASE_URL/api/v1/games")

GAME_ID=$(jq -er '.state.gameId' <<<"$GAME_JSON")
printf '%s\n' "$GAME_ID"
```

Give the printed `GAME_ID` to the other player. There is no
`GET /api/v1/games` listing endpoint; do not try to discover the created game by
probing that path.

### Option B: Join an existing game

```bash
set -euo pipefail

BASE_URL="http://localhost:8080"
COOKIE_JAR="./agent.cookies"
GAME_ID="replace-with-host-game-id"
touch "$COOKIE_JAR"

curl --fail-with-body --silent --show-error \
  -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  "$BASE_URL/api/v1/auth/me" | jq -e . >/dev/null

CSRF=$(curl --fail-with-body --silent --show-error \
  -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  "$BASE_URL/api/v1/auth/csrf" | jq -er '.token')

curl --fail-with-body --silent --show-error \
  -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  -H "X-XSRF-TOKEN: $CSRF" \
  -X PUT "$BASE_URL/api/v1/games/$GAME_ID/players/me" | jq -e .
```

## Agent Decision Loop (Game-Agnostic)

1. Fetch rules once (or when game changes):

```bash
RULES_JSON=$(curl --fail-with-body --silent --show-error \
  -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  "$BASE_URL/api/v1/games/$GAME_ID/rules")
```

2. Fetch current state:

```bash
STATE_JSON=$(curl --fail-with-body --silent --show-error \
  -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  "$BASE_URL/api/v1/games/$GAME_ID/state")
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

CSRF=$(curl --fail-with-body --silent --show-error \
  -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  "$BASE_URL/api/v1/auth/csrf" | jq -er '.token')

RESULT=$(curl --fail-with-body --silent --show-error \
  -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
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
EVENTS=$(curl --fail-with-body --silent --show-error \
  -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
  "$BASE_URL/api/v1/games/$GAME_ID/events?afterSequence=$AFTER_SEQ")
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

- Empty `403`: usually no authenticated session or invalid CSRF. Call `/auth/me`
  with both `-b` and `-c`. If that fails, log in again. Otherwise refresh CSRF
  with both `-b` and `-c`, then retry the mutation once.
- ProblemDetail `403`: authenticated actor is not a participant in that game.
- `404`: game does not exist.
- `409`: conflict (for example, join when game is full).
- `400`: invalid request shape or parameters.
- Never use plain `curl -s`; use `--fail-with-body --silent --show-error` so an
  HTTP error cannot be mistaken for an empty successful response.

## Multi-Agent / Two-User Testing Pattern

- Use one cookie jar per actor (for example `user_a.cookies`, `user_b.cookies`).
- Each actor runs the same loop independently against the same `gameId`.
- Never share cookie jars between actors.

## What to Provide When Prompting an Agent

When you tell an autonomous agent to "go", provide:

1. path to the runtime text file (for example `docs/agent-client-runtime.txt`)
2. objective (for example "play to win" or another external win condition)

## Suggested Prompt Snippet

"Use `docs/agent-client-skill.md` as your execution guide. First read runtime values from `docs/agent-client-runtime.txt` (including `BASE_URL`, `MODE`, and `GAME_ID` when `MODE=join`). Authenticate, join or create based on `MODE`, fetch `/rules`, and only submit actions that satisfy the returned schema. Continue until terminal game state or max turn budget. Log every request/response summary, and at completion write a detailed log of all game events (including turn ownership and host/opponent designation) to the configured `EVENT_LOG_PATH`, creating the parent directory if needed."
