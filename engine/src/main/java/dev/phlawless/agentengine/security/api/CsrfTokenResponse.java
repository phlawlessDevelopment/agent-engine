package dev.phlawless.agentengine.security.api;

public record CsrfTokenResponse(String token, String headerName, String parameterName) {
}
