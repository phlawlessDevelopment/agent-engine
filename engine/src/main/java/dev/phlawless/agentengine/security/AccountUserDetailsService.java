package dev.phlawless.agentengine.security;

import dev.phlawless.agentengine.account.application.AccountService;
import dev.phlawless.agentengine.account.domain.Account;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class AccountUserDetailsService implements UserDetailsService {
    private final AccountService accountService;

    public AccountUserDetailsService(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            Account account = accountService.requireByUsername(username);
            return new AuthenticatedAccount(account.id(), account.username(), account.passwordHash());
        } catch (RuntimeException ex) {
            throw new UsernameNotFoundException("Invalid username or password");
        }
    }
}
