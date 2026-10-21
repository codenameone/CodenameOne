package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;

/// What a new player sends to open an account.
@Mapped
public record RegisterDto(String email, String password, String displayName) {
}
