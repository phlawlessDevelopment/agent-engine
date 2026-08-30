package dev.phlawless.agentengine.examples;

import dev.phlawless.agentengine.game.application.GameRepository;
import dev.phlawless.agentengine.game.infrastructure.InMemoryGameRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AgentEngineExamplesApplicationTests {

	@Autowired
	private GameRepository gameRepository;

	@Test
	void contextLoads() {
		assertThat(gameRepository).isInstanceOf(InMemoryGameRepository.class);
	}

}
