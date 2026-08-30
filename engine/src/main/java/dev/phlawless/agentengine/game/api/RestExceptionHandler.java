package dev.phlawless.agentengine.game.api;

import dev.phlawless.agentengine.account.application.UsernameAlreadyExistsException;
import dev.phlawless.agentengine.game.application.GameFullException;
import dev.phlawless.agentengine.game.application.GameNotFoundException;
import dev.phlawless.agentengine.game.application.GameNotReadyException;
import dev.phlawless.agentengine.game.application.NotGameParticipantException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestExceptionHandler {
    @ExceptionHandler(GameNotFoundException.class)
    ProblemDetail handleGameNotFound(GameNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Game not found");
        problem.setType(URI.create("https://agent-engine.dev/problems/game-not-found"));
        return problem;
    }

    @ExceptionHandler(NotGameParticipantException.class)
    ProblemDetail handleNotGameParticipant(NotGameParticipantException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
        problem.setTitle("Forbidden");
        problem.setType(URI.create("https://agent-engine.dev/problems/not-game-participant"));
        return problem;
    }

    @ExceptionHandler(GameFullException.class)
    ProblemDetail handleGameFull(GameFullException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Game is full");
        problem.setType(URI.create("https://agent-engine.dev/problems/game-full"));
        return problem;
    }

    @ExceptionHandler(GameNotReadyException.class)
    ProblemDetail handleGameNotReady(GameNotReadyException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Game not ready");
        problem.setType(URI.create("https://agent-engine.dev/problems/game-not-ready"));
        return problem;
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    ProblemDetail handleUsernameAlreadyExists(UsernameAlreadyExistsException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Username already exists");
        problem.setType(URI.create("https://agent-engine.dev/problems/username-already-exists"));
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid request");
        problem.setType(URI.create("https://agent-engine.dev/problems/invalid-request"));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Validation error");
        problem.setType(URI.create("https://agent-engine.dev/problems/validation-error"));
        List<String> violations = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .toList();
        problem.setProperty("violations", violations);
        return problem;
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
