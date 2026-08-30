package dev.phlawless.agentengine.game.api;

import java.util.List;

public record SubmitActionResponse(
        boolean accepted,
        String message,
        ObservableStateResponse state,
        List<EventResponse> events
) {
}
