package dev.phlawless.agentengine.account.application;

import dev.phlawless.agentengine.account.domain.Account;
import dev.phlawless.agentengine.account.infrastructure.InMemoryAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountServiceTest {
    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void registerNormalizesUsernameAndHashesPassword() {
        AccountService service = new AccountService(new InMemoryAccountRepository(), passwordEncoder, clock);

        Account account = service.register("Alice", "supersecurepw");

        assertThat(account.username()).isEqualTo("alice");
        assertThat(account.createdAt()).isEqualTo(Instant.parse("2026-08-30T12:00:00Z"));
        assertThat(account.passwordHash()).isNotEqualTo("supersecurepw");
        assertThat(passwordEncoder.matches("supersecurepw", account.passwordHash())).isTrue();
    }

    @Test
    void duplicateUsernamesAreCaseInsensitive() {
        AccountService service = new AccountService(new InMemoryAccountRepository(), passwordEncoder, clock);

        service.register("Bob", "anothersecurepw");

        assertThatThrownBy(() -> service.register("bob", "yetanothersecurepw"))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    void passwordMustMeetMinimumLength() {
        AccountService service = new AccountService(new InMemoryAccountRepository(), passwordEncoder, clock);

        assertThatThrownBy(() -> service.register("carol", "short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 12");
    }
}
