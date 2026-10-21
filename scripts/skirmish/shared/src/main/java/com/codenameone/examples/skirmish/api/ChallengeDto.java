package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;

/// A private match invitation. Whoever opens `url` (it opens the web build) joins this match.
@Mapped
public record ChallengeDto(String code, String url, int expiresInSeconds) {
}
