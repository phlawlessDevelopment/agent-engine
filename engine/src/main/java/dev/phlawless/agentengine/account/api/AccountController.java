package dev.phlawless.agentengine.account.api;

import dev.phlawless.agentengine.account.application.AccountService;
import dev.phlawless.agentengine.account.domain.Account;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse register(@Valid @RequestBody RegisterAccountRequest request) {
        Account created = accountService.register(request.username(), request.password());
        return new AccountResponse(created.id(), created.username(), created.createdAt());
    }
}
