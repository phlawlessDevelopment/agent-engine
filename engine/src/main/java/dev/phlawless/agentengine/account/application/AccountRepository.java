package dev.phlawless.agentengine.account.application;

import dev.phlawless.agentengine.account.domain.Account;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Optional<Account> findById(UUID accountId);

    Optional<Account> findByUsername(String normalizedUsername);

    boolean saveIfUsernameAvailable(Account account);
}
