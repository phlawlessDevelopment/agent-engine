package dev.phlawless.agentengine.examples;

import dev.phlawless.agentengine.examples.tictactoe.TicTacToeRules;
import dev.phlawless.agentengine.game.domain.GameRules;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AgentEngineExamplesApplication {

	@Bean
	public GameRules activeGameRules() {
		return new TicTacToeRules();
	}

	public static void main(String[] args) {
		SpringApplication.run(AgentEngineExamplesApplication.class, args);
	}

}
