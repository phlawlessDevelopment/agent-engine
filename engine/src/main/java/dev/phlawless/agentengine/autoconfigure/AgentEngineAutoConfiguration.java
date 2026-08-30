package dev.phlawless.agentengine.autoconfigure;

import dev.phlawless.agentengine.game.api.GameController;
import dev.phlawless.agentengine.game.api.RestExceptionHandler;
import dev.phlawless.agentengine.game.application.GameRepository;
import dev.phlawless.agentengine.game.application.GameService;
import dev.phlawless.agentengine.game.domain.GameRules;
import dev.phlawless.agentengine.game.infrastructure.InMemoryGameRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AgentEngineAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    Clock gameClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean(GameRepository.class)
    GameRepository gameRepository() {
        return new InMemoryGameRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    GameService gameService(GameRepository gameRepository, GameRules gameRules, Clock clock) {
        return new GameService(gameRepository, gameRules, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    GameController gameController(GameService gameService) {
        return new GameController(gameService);
    }

    @Bean
    @ConditionalOnMissingBean
    RestExceptionHandler restExceptionHandler() {
        return new RestExceptionHandler();
    }
}
