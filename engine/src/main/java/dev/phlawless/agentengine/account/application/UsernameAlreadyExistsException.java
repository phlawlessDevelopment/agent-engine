package dev.phlawless.agentengine.account.application;

public final class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException(String username) {
        super("Username is already taken: " + username);
    }
}
