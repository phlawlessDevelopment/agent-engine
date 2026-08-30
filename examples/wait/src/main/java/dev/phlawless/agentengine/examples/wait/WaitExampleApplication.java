package dev.phlawless.agentengine.examples.wait;

import dev.phlawless.agentengine.game.domain.GameRules;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WaitExampleApplication {

    @Bean
    public GameRules activeGameRules() {
        return new WaitRules();
    }

    public static void main(String[] args) {
        SpringApplication.run(WaitExampleApplication.class, args);
    }
}
