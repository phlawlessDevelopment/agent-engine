package dev.phlawless.agentengine.autoconfigure;

import dev.phlawless.agentengine.account.api.AccountController;
import dev.phlawless.agentengine.account.application.AccountRepository;
import dev.phlawless.agentengine.account.application.AccountService;
import dev.phlawless.agentengine.account.infrastructure.InMemoryAccountRepository;
import dev.phlawless.agentengine.game.api.GameController;
import dev.phlawless.agentengine.game.api.RestExceptionHandler;
import dev.phlawless.agentengine.game.application.GameRepository;
import dev.phlawless.agentengine.game.application.GameService;
import dev.phlawless.agentengine.game.domain.GameRules;
import dev.phlawless.agentengine.game.infrastructure.InMemoryGameRepository;
import dev.phlawless.agentengine.security.AccountUserDetailsService;
import dev.phlawless.agentengine.security.api.AuthenticationController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

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
    @ConditionalOnMissingBean(AccountRepository.class)
    AccountRepository accountRepository() {
        return new InMemoryAccountRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    AccountService accountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder, Clock clock) {
        return new AccountService(accountRepository, passwordEncoder, clock);
    }

    @Bean
    @ConditionalOnMissingBean(UserDetailsService.class)
    UserDetailsService userDetailsService(AccountService accountService) {
        return new AccountUserDetailsService(accountService);
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationManager.class)
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    @ConditionalOnMissingBean(SecurityContextRepository.class)
    SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository());
    }

    @Bean
    @ConditionalOnMissingBean(SessionAuthenticationStrategy.class)
    SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository) throws Exception {
        http
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .securityContext(securityContext -> securityContext.securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/accounts").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)));
        return http.build();
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
    AccountController accountController(AccountService accountService) {
        return new AccountController(accountService);
    }

    @Bean
    @ConditionalOnMissingBean
    AuthenticationController authenticationController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy
    ) {
        return new AuthenticationController(authenticationManager, securityContextRepository, sessionAuthenticationStrategy);
    }

    @Bean
    @ConditionalOnMissingBean
    RestExceptionHandler restExceptionHandler() {
        return new RestExceptionHandler();
    }
}
