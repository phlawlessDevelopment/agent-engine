package dev.phlawless.agentengine.autoconfigure;

import dev.phlawless.agentengine.game.api.GameController;
import dev.phlawless.agentengine.game.api.RestExceptionHandler;
import dev.phlawless.agentengine.game.application.GameRepository;
import dev.phlawless.agentengine.game.application.GameService;
import dev.phlawless.agentengine.game.domain.Command;
import dev.phlawless.agentengine.game.domain.GameRules;
import dev.phlawless.agentengine.game.domain.GameState;
import dev.phlawless.agentengine.game.domain.RuleResult;
import dev.phlawless.agentengine.game.infrastructure.InMemoryGameRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AgentEngineAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AgentEngineAutoConfiguration.class));

    @Test
    void autoConfigurationCreatesDefaultBeansWithSingleGameRules() {
        contextRunner.withUserConfiguration(SingleRulesConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(GameRules.class);
                    assertThat(context).hasSingleBean(GameRepository.class);
                    assertThat(context).hasSingleBean(InMemoryGameRepository.class);
                    assertThat(context).hasSingleBean(Clock.class);
                    assertThat(context).hasSingleBean(GameService.class);
                    assertThat(context).hasSingleBean(GameController.class);
                    assertThat(context).hasSingleBean(RestExceptionHandler.class);
                });
    }

    @Test
    void customRepositoryOverridesDefaultInMemoryRepository() {
        contextRunner.withUserConfiguration(SingleRulesConfig.class, CustomRepositoryConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(GameRepository.class);
                    assertThat(context).doesNotHaveBean(InMemoryGameRepository.class);
                    assertThat(context.getBean(GameRepository.class)).isInstanceOf(CustomGameRepository.class);
                });
    }

    @Test
    void customClockOverridesDefaultClock() {
        contextRunner.withUserConfiguration(SingleRulesConfig.class, CustomClockConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(Clock.class);
                    Clock clock = context.getBean(Clock.class);
                    assertThat(clock.instant()).isEqualTo(Instant.parse("2026-08-30T12:00:00Z"));
                });
    }

    @Test
    void customGameServiceDisablesDefaultServiceBean() {
        contextRunner.withUserConfiguration(SingleRulesConfig.class, CustomGameServiceConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(GameService.class);
                    assertThat(context.getBean(GameService.class)).isSameAs(CustomGameServiceConfig.CUSTOM_SERVICE);
                });
    }

    @Test
    void contextFailsWithoutGameRulesBean() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasMessageContaining("GameRules");
        });
    }

    @Test
    void contextFailsWithMultipleGameRulesBeans() {
        contextRunner.withUserConfiguration(MultipleRulesConfig.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessageContaining("GameRules");
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class SingleRulesConfig {
        @Bean
        GameRules gameRules() {
            return new StubGameRules();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class MultipleRulesConfig {
        @Bean
        GameRules firstGameRules() {
            return new StubGameRules();
        }

        @Bean
        GameRules secondGameRules() {
            return new StubGameRules();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomRepositoryConfig {
        @Bean
        GameRepository gameRepository() {
            return new CustomGameRepository();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomClockConfig {
        @Bean
        Clock gameClock() {
            return Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomGameServiceConfig {
        private static final GameService CUSTOM_SERVICE = new GameService(
                new InMemoryGameRepository(),
                new StubGameRules(),
                Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZoneOffset.UTC));

        @Bean
        GameService gameService() {
            return CUSTOM_SERVICE;
        }
    }

    static final class CustomGameRepository implements GameRepository {
        @Override
        public java.util.Optional<dev.phlawless.agentengine.game.domain.Game> findById(UUID gameId) {
            return java.util.Optional.empty();
        }

        @Override
        public void save(dev.phlawless.agentengine.game.domain.Game game) {
        }
    }

    static final class StubGameRules implements GameRules {
        @Override
        public Set<String> actionTypes() {
            return Set.of("WAIT");
        }

        @Override
        public GameState initialState() {
            return StubGameState.INSTANCE;
        }

        @Override
        public RuleResult evaluate(GameState state, Command command, int turn, Instant now) {
            return RuleResult.reject("Not implemented");
        }
    }

    enum StubGameState implements GameState {
        INSTANCE;

        @Override
        public Map<String, Object> toObservable() {
            return Map.of();
        }
    }
}
