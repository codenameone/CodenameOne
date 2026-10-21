package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;

/// The parts of a profile a player may change.
@Mapped
public record ProfileDto(String displayName) {
}
