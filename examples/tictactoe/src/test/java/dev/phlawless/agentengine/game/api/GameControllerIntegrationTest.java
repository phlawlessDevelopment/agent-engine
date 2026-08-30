package dev.phlawless.agentengine.game.api;

import com.jayway.jsonpath.JsonPath;
import dev.phlawless.agentengine.examples.tictactoe.TicTacToeExampleApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TicTacToeExampleApplication.class)
@AutoConfigureMockMvc
class GameControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(post("/api/v1/games"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/games/{gameId}/state", "7f857e4b-a56d-4df7-a479-0fac4d528c8a"))
                .andExpect(status().isForbidden());
    }

    @Test
    void twoUsersCanCreateJoinAndPlay() throws Exception {
        MockHttpSession aliceSession = registerAndLogin("alice", "supersecurepw");
        MockHttpSession bobSession = registerAndLogin("bob", "anothersecurepw");

        String gameId = mockMvc.perform(post("/api/v1/games")
                        .session(aliceSession)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state.players[0].username").value("alice"))
                .andExpect(jsonPath("$.state.ready").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String createdGameId = JsonPath.read(gameId, "$.state.gameId");

        mockMvc.perform(put("/api/v1/games/{gameId}/players/me", createdGameId)
                        .session(bobSession)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state.ready").value(true))
                .andExpect(jsonPath("$.state.players.length()").value(2));

        mockMvc.perform(get("/api/v1/games/{gameId}/rules", createdGameId)
                        .session(aliceSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.game").value("TicTacToe"))
                .andExpect(jsonPath("$.actions[0].type").value("PLACE_MARKER"))
                .andExpect(jsonPath("$.actions[0].payload.position.type").value("integer"))
                .andExpect(jsonPath("$.actions[0].payload.position.constraints.min").value(0))
                .andExpect(jsonPath("$.actions[0].payload.position.constraints.max").value(8));

        mockMvc.perform(post("/api/v1/games/{gameId}/actions", createdGameId)
                        .session(bobSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"PLACE_MARKER\",\"payload\":{\"position\":0}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(false))
                .andExpect(jsonPath("$.message").value("It is not your turn"));

        mockMvc.perform(post("/api/v1/games/{gameId}/actions", createdGameId)
                        .session(aliceSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"PLACE_MARKER\",\"payload\":{\"position\":0}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.state.turn").value(1));

        mockMvc.perform(get("/api/v1/games/{gameId}/events", createdGameId)
                        .session(aliceSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("GAME_CREATED"))
                .andExpect(jsonPath("$[1].type").value("PLAYER_JOINED"))
                .andExpect(jsonPath("$[2].actorSeat").value(0));
    }

    @Test
    void thirdUserCannotJoinOrReadWhenNotParticipant() throws Exception {
        MockHttpSession aliceSession = registerAndLogin("alice2", "supersecurepw");
        MockHttpSession bobSession = registerAndLogin("bob2", "anothersecurepw");
        MockHttpSession carolSession = registerAndLogin("carol2", "yetanothersecurepw");

        String gamePayload = mockMvc.perform(post("/api/v1/games")
                        .session(aliceSession)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String gameId = JsonPath.read(gamePayload, "$.state.gameId");

        mockMvc.perform(put("/api/v1/games/{gameId}/players/me", gameId)
                        .session(bobSession)
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/games/{gameId}/players/me", gameId)
                        .session(carolSession)
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Game is full"));

        mockMvc.perform(get("/api/v1/games/{gameId}/state", gameId)
                        .session(carolSession))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/games/{gameId}/rules", gameId)
                        .session(carolSession))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession registerAndLogin(String username, String password) throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/v1/accounts")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));

        return session;
    }
}
