I have this idea for a video game engine / server where on client side input is done by writing to text files (the server will have all the rules / state , so people can't cheat) but the idea is that the players could point Claude code or codex etc at the game directory and create custom agents / skills that play the game. I haven't landed on any specific game idea, Does that make sense so far ? 

Yes — it makes a lot of sense, and I think there’s actually a pretty interesting idea hiding in it.

What you're describing is roughly:

A game where the interface between the player and the game is a filesystem/API, rather than a traditional GUI/controller — deliberately designed so coding agents can operate the game.

The important distinction is that the client is not trusted.

The architecture I think you're describing

Something like:

                    ┌──────────────────────────┐
                    │       GAME SERVER        │
                    │                          │
                    │  • World state           │
                    │  • Rules / physics       │
                    │  • NPCs                   │
                    │  • Validation             │
                    │  • RNG                    │
                    │  • Win/loss conditions    │
                    └────────────┬─────────────┘
                                 │
                         State / events
                                 │
                    ┌────────────▼─────────────┐
                    │       GAME DIRECTORY     │
                    │                          │
                    │  state/                  │
                    │    player.json           │
                    │    world.json             │
                    │                          │
                    │  input/                  │
                    │    action.txt            │
                    │                          │
                    │  output/                 │
                    │    result.json            │
                    │    events.log            │
                    └────────────┬─────────────┘
                                 │
                    ┌────────────▼─────────────┐
                    │     PLAYER / AI AGENT    │
                    │                          │
                    │ Claude Code / Codex /    │
                    │ custom Python / shell /  │
                    │ whatever the player wants│
                    └──────────────────────────┘

The filesystem becomes the game controller.

For example, an agent might see:

game/
├── README.md
├── rules.md
├── state/
│   ├── player.json
│   ├── inventory.json
│   └── visible_world.json
├── input/
│   └── action.txt
├── output/
│   ├── result.json
│   └── events.log
└── skills/
    └── ...

And the agent could do:

echo "move north" > input/action.txt

The server receives that and decides:

VALID ACTION
→ Player moved north
→ Encountered Goblin
→ Combat initiated

The client cannot simply edit:

player.health = 999999
inventory.gold = 999999

because those aren't authoritative.

And I think the really interesting part is the "Claude Code / Codex" aspect

You aren't necessarily making a game with AI.

You're making a game where:

AI agents are first-class players.

That's a subtly different and, IMO, much more interesting concept.

A human could play manually:

input/action.txt

attack goblin

A simple script could play:

while True:
    state = read_state()
    action = choose_action(state)
    write_action(action)

Or someone could point Claude Code at the directory and say:

"Figure out how this game works and build me an agent that survives for as long as possible."

And Claude gets access to things like:

README.md
rules.md
state/
skills/

It could then create:

my_agent/
├── agent.py
├── skills/
│   ├── exploration.md
│   ├── combat.md
│   └── resource_management.md
└── strategy.md

That creates an interesting meta-game around developing increasingly sophisticated agents.

The key design principle

I would strongly recommend separating:

1. Authoritative state

Only the server can change this.

{
  "player": {
    "hp": 73,
    "position": [12, 8],
    "gold": 41
  }
}
2. Observable state

What the player/agent is allowed to know.

{
  "position": [12, 8],
  "visible_tiles": [...],
  "nearby_entities": [...],
  "known_quests": [...]
}
3. Commands

What the client is allowed to request.

MOVE north
ATTACK goblin_42
USE potion_3
TALK merchant_7

The server effectively does:

                 REQUEST
                    │
                    ▼
             ┌─────────────┐
             │ Is this a   │
             │ valid action│
             │ right now?  │
             └──────┬──────┘
                    │
              ┌─────┴─────┐
             NO           YES
              │             │
           reject       apply rule
                            │
                            ▼
                      update state
                            │
                            ▼
                       emit event

That gives you a deterministic, inspectable game protocol.

There's another really cool possibility

You don't even necessarily need to expose a conventional game UI initially.

Imagine a game that ships with:

GAME/
├── game.md
├── rules/
├── state/
├── actions/
└── examples/

You tell the player:

"You have been dropped into this world. Figure it out."

The game itself becomes almost like a programming environment / simulation.

Different people could build radically different clients:

                 GAME SERVER
                      │
          ┌───────────┼───────────┐
          │           │           │
       Human        Claude       Codex
       Client       Agent        Agent
          │           │           │
       keyboard     Python      TypeScript

All three are playing the same game.

And this opens up some genuinely interesting game designs

You don't need to decide the game yet.

In fact, I'd probably design the engine/protocol first and let the game emerge from it.

Some particularly compatible genres would be:

Colony / settlement simulation — agents manage resources and populations.
Space exploration — agents control ships and make strategic decisions.
Trading/economy game — agents develop trading strategies.
Survival game — limited information makes planning important.
Dungeon crawler — agents explore and fight autonomously.
Grand strategy — each player controls a faction.
Detective/mystery — agents have to investigate and reason about evidence.
Automation game — the player's actual "character" is their agent.
Programming/engineering game — players literally develop increasingly capable agents.
Multiplayer civilisation simulation — potentially very interesting with human and AI players together.

The last one especially could become something quite unusual:

A persistent world populated by humans, scripts, LLM agents, and combinations of the three.

And because the server is authoritative, you don't have to trust the clients.

One thing I'd change about the "text files" idea

I'd keep text files as the human/agent-facing interface, but underneath I'd probably make it a proper protocol.

For example:

input/
    command.json

output/
    response.json

events/
    000001.json
    000002.json

while providing a very simple abstraction:

game move north
game attack goblin
game inspect

That means Claude Code can interact with it naturally, but you aren't permanently committing the engine to filesystem polling.

You could even support:

Filesystem
     │
     ├── CLI
     ├── Python SDK
     ├── REST
     ├── WebSocket
     └── MCP

all talking to the same underlying game protocol.

MCP in particular could make this concept really interesting, because you could potentially expose the game as tools/resources that coding agents already understand.

So yes: I absolutely understand the idea, and I think the core concept is stronger than "a game controlled by text files."

I'd describe the underlying concept as:

An authoritative game simulation with an agent-native interface, where players are free to build their own software agents to interact with the world.

And I think the next interesting question isn't "what game should it be?" — it's "what would the minimum viable game protocol need to look like to make Claude/Codex genuinely fun to play against?"

Okay, sweet... So for career progression reasons I'd like to build the initial prototype server using springboot.. is that a terrible idea ? 

No — I actually think Spring Boot is a very good choice for this prototype, especially given your career goal.

You're not trying to prove that Spring Boot is the most performant possible game-server technology. You're trying to build something that demonstrates:

Java/Spring Boot backend development
REST/API design
authentication/authorisation
state management
persistence
concurrency
testing
Docker/containerisation
potentially WebSockets/event-driven architecture
and, eventually, an interesting AI-agent integration layer

That's a very employable portfolio project.

I'd structure the prototype roughly like this
                 ┌───────────────────────────┐
                 │       Spring Boot         │
                 │                           │
                 │   Game Engine             │
                 │   ├── Rules               │
                 │   ├── World State         │
                 │   ├── Player State        │
                 │   ├── Actions             │
                 │   └── Game Loop           │
                 │                           │
                 │   API                     │
                 │   ├── GET /state          │
                 │   ├── POST /actions       │
                 │   └── GET /events         │
                 └─────────────┬─────────────┘
                               │
                         PostgreSQL

Then have a deliberately dumb client:

game/
├── README.md
├── state/
│   └── state.json
├── input/
│   └── action.txt
└── output/
    └── result.json

The filesystem client talks to the Spring API.

That separation is important because eventually you could have:

                   Spring Boot
                  Game Server
                       │
          ┌────────────┼────────────┐
          │            │            │
       CLI client   Claude Agent  Codex Agent
          │            │            │
       Python       Python/Java    Whatever

And none of those clients get to decide what actually happens.

One architectural decision I'd make early

Don't put the game logic inside your controllers.

Avoid:

@PostMapping("/action")
public ResponseEntity<?> action(...) {
    // move player
    // check enemy
    // calculate damage
    // update inventory
    // ...
}

Instead:

Controller
    ↓
GameService
    ↓
GameEngine
    ↓
Domain Model

Something like:

public ActionResult execute(GameId gameId, PlayerId playerId, Action action) {
    GameState state = repository.load(gameId);

    ActionResult result = rules.validate(state, action);

    if (result.isValid()) {
        state.apply(action);
        repository.save(state);
    }

    return result;
}

That gives you a nice boundary where the game engine itself doesn't care whether the request came from a human, Python script, Claude, or Codex.

And there's a really nice progression for the project

I'd build it in stages:

Phase 1 — Tiny simulation

World
 ├── Player
 ├── Rooms
 ├── Items
 └── Actions

REST API:

GET  /games/{id}/state
POST /games/{id}/actions

Phase 2 — Persistence

PostgreSQL + Spring Data JPA.

Phase 3 — Events

PlayerMoved
ItemPickedUp
EnemyAttacked
PlayerDied

Potentially Spring's application events initially, then something like Kafka/RabbitMQ later if you want to demonstrate that technology.

Phase 4 — Agent interface

Create a directory that exposes the game as files:

game/
├── README.md
├── rules.md
├── state.json
├── actions/
└── events/

Phase 5 — Give Claude/Codex the directory.

And this is where the project gets really interesting.

You could literally document the challenge as:

"Build an autonomous agent capable of surviving 100 turns."

Then have the agent write its own code/skills against your API.

And honestly, I'd resist the temptation to make the first game complicated.

A tiny world such as:

A ── B ── C
    │
    D

with:

MOVE
LOOK
PICKUP
ATTACK
REST

is enough.

If the underlying architecture is good, you can make the game itself much more sophisticated later.

Spring Boot isn't the thing I'd worry about. The interesting engineering challenge is designing a clean authoritative simulation + agent-facing protocol. Spring Boot is perfectly capable of being the first implementation of that.
