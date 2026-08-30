package dev.phlawless.agentengine.account.application;

public final class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
