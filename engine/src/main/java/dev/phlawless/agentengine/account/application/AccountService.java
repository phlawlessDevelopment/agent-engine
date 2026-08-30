package dev.phlawless.agentengine.account.application;

import dev.phlawless.agentengine.account.domain.Account;
import dev.phlawless.agentengine.account.domain.AccountIdentity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public class AccountService {
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_PASSWORD_BCRYPT_BYTES = 72;

    private final AccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AccountService(AccountRepository repository, PasswordEncoder passwordEncoder, Clock clock) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public Account register(String username, String rawPassword) {
        String normalized = normalizeUsername(username);
        validateUsername(normalized);
        validatePassword(rawPassword);

        Account account = new Account(
                UUID.randomUUID(),
                normalized,
                passwordEncoder.encode(rawPassword),
                Instant.now(clock));

        boolean saved = repository.saveIfUsernameAvailable(account);
        if (!saved) {
            throw new UsernameAlreadyExistsException(normalized);
        }
        return account;
    }

    public Account requireByUsername(String username) {
        String normalized = normalizeUsername(username);
        return repository.findByUsername(normalized)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + normalized));
    }

    public Account requireById(UUID accountId) {
        return repository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
    }

    public AccountIdentity identityFor(UUID accountId) {
        Account account = requireById(accountId);
        return new AccountIdentity(account.id(), account.username());
    }

    public static String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private void validateUsername(String username) {
        if (username.length() < MIN_USERNAME_LENGTH || username.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("Username must be between 3 and 50 characters");
        }
        if (!username.matches("[a-z0-9._-]+")) {
            throw new IllegalArgumentException("Username may only contain letters, numbers, '.', '_' and '-'");
        }
    }

    private void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least 12 characters");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BCRYPT_BYTES) {
            throw new IllegalArgumentException("Password exceeds bcrypt maximum byte length");
        }
    }
}
