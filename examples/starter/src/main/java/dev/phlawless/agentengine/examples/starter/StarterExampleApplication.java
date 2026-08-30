package dev.phlawless.agentengine.examples.starter;

import dev.phlawless.agentengine.game.domain.GameRules;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class StarterExampleApplication {

    @Bean
    public GameRules activeGameRules() {
        return new StarterGameRules();
    }

    public static void main(String[] args) {
        SpringApplication.run(StarterExampleApplication.class, args);
    }
}
