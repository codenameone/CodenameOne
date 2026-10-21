package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;

/// One row of the leaderboard.
@Mapped
public record LeaderDto(int rank, String displayName, int trophies, int wins) {
}
