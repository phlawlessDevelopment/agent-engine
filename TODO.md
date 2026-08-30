# TODO

- [x] Pluggable rule modules (`GameRules` SPI + registry)
- [x] Reference modules: `wait`, `tictactoe` (worked example of the SPI)
- [ ] Persist sessions/events to sqlite (currently in-memory only)
- [ ] Add filesystem/CLI client adapter for agent workflows
- [ ] Add authentication and player identity boundaries
- [ ] Strengthen startup validation so exactly one `GameRules` bean is active per deployment
- [ ] Command/event payload types: consider moving from `Map<String,Object>` /
      `Map<String,String>` to a schema-per-game-type contract
- [ ] Long-polling or SSE option for `/events` so agents don't busy-poll
