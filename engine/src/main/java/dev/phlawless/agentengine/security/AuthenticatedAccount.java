package dev.phlawless.agentengine.security;

import dev.phlawless.agentengine.account.domain.AccountIdentity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class AuthenticatedAccount implements UserDetails {
    private static final List<GrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final UUID accountId;
    private final String username;
    private final String passwordHash;

    public AuthenticatedAccount(UUID accountId, String username, String passwordHash) {
        this.accountId = accountId;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public UUID accountId() {
        return accountId;
    }

    public AccountIdentity toIdentity() {
        return new AccountIdentity(accountId, username);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return AUTHORITIES;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
