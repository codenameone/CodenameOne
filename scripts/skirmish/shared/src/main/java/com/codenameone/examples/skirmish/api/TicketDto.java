package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;

/// A one-shot ticket that opens the game socket; a socket cannot carry a bearer token on every platform.
@Mapped
public record TicketDto(String ticket, int expiresInSeconds) {
}
