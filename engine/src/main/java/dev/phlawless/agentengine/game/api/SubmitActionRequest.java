package dev.phlawless.agentengine.game.api;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record SubmitActionRequest(
        @NotBlank String type,
        Map<String, Object> payload
) {
}
