package dev.phlawless.agentengine.examples.chess;

import dev.phlawless.agentengine.game.domain.GameRules;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ChessExampleApplication {

    @Bean
    public GameRules activeGameRules() {
        return new ChessRules();
    }

    public static void main(String[] args) {
        SpringApplication.run(ChessExampleApplication.class, args);
    }
}
