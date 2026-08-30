package dev.phlawless.agentengine.examples.tictactoe;

import dev.phlawless.agentengine.game.domain.GameRules;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TicTacToeExampleApplication {

    @Bean
    public GameRules activeGameRules() {
        return new TicTacToeRules();
    }

    public static void main(String[] args) {
        SpringApplication.run(TicTacToeExampleApplication.class, args);
    }
}
