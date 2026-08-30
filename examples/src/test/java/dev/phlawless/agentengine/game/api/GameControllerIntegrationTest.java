package dev.phlawless.agentengine.game.api;

import dev.phlawless.agentengine.game.application.GameService;
import dev.phlawless.agentengine.examples.AgentEngineExamplesApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AgentEngineExamplesApplication.class)
@AutoConfigureMockMvc
class GameControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameService gameService;

    @Test
    void createGameEndpointReturnsCreatedTicTacToeGame() throws Exception {
        mockMvc.perform(post("/api/v1/games"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state.gameId").isString())
                .andExpect(jsonPath("$.state.gameType").value("tictactoe"))
                .andExpect(jsonPath("$.state.actions[0]").value("PLACE_MARKER"))
                .andExpect(jsonPath("$.state.turn").value(0))
                .andExpect(jsonPath("$.state.state.board.length()").value(9))
                .andExpect(jsonPath("$.state.state.currentPlayer").value("X"));
    }

    @Test
    void createGameWithExplicitGameTypeUsesThatModule() throws Exception {
        mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameType\":\"wait\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state.gameType").value("wait"))
                .andExpect(jsonPath("$.state.actions[0]").value("WAIT"));
    }

    @Test
    void createGameWithUnknownGameTypeReturnsProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameType\":\"nope\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Unknown game type"));
    }

    @Test
    void gameLifecycleWorksForStateActionAndEvents() throws Exception {
        String gameId = gameService.createGame().gameId().toString();

        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turn").value(0));

        mockMvc.perform(
                        post("/api/v1/games/{gameId}/actions", gameId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"type\":\"PLACE_MARKER\",\"payload\":{\"position\":0}}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.state.turn").value(1))
                .andExpect(jsonPath("$.state.state.currentPlayer").value("O"))
                .andExpect(jsonPath("$.events[0].type").value("MARKER_PLACED"));

        mockMvc.perform(get("/api/v1/games/{gameId}/events", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("GAME_CREATED"));
    }

    @Test
    void rejectedActionReturnsAcceptedFalseWithMessage() throws Exception {
        String gameId = gameService.createGame().gameId().toString();

        mockMvc.perform(
                        post("/api/v1/games/{gameId}/actions", gameId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"type\":\"PLACE_MARKER\",\"payload\":{\"position\":99}}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(false))
                .andExpect(jsonPath("$.message").value("Position must be an integer between 0 and 8"))
                .andExpect(jsonPath("$.events.length()").value(0));
    }

    @Test
    void actionWithoutTypeFailsValidation() throws Exception {
        String gameId = gameService.createGame().gameId().toString();

        mockMvc.perform(
                        post("/api/v1/games/{gameId}/actions", gameId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"payload\":{}}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"));
    }

    @Test
    void missingGameReturnsProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/games/{gameId}/state", "7f857e4b-a56d-4df7-a479-0fac4d528c8a"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Game not found"));
    }
}
