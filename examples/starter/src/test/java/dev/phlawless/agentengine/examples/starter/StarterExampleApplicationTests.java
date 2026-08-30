package dev.phlawless.agentengine.examples.starter;

import dev.phlawless.agentengine.game.domain.GameRules;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StarterExampleApplicationTests {

    @Autowired
    private GameRules gameRules;

    @Test
    void contextLoadsStarterRules() {
        assertThat(gameRules).isInstanceOf(StarterGameRules.class);
    }
}
