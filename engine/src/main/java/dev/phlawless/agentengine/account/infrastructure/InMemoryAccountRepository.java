package dev.phlawless.agentengine.account.infrastructure;

import dev.phlawless.agentengine.account.application.AccountRepository;
import dev.phlawless.agentengine.account.domain.Account;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryAccountRepository implements AccountRepository {
    private final ConcurrentMap<UUID, Account> accountsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> accountIdByUsername = new ConcurrentHashMap<>();

    @Override
    public Optional<Account> findById(UUID accountId) {
        return Optional.ofNullable(accountsById.get(accountId));
    }

    @Override
    public Optional<Account> findByUsername(String normalizedUsername) {
        UUID accountId = accountIdByUsername.get(normalizedUsername);
        if (accountId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(accountsById.get(accountId));
    }

    @Override
    public boolean saveIfUsernameAvailable(Account account) {
        UUID existing = accountIdByUsername.putIfAbsent(account.username(), account.id());
        if (existing != null) {
            return false;
        }

        Account previous = accountsById.putIfAbsent(account.id(), account);
        if (previous != null) {
            accountIdByUsername.remove(account.username(), account.id());
            return false;
        }
        return true;
    }
}
