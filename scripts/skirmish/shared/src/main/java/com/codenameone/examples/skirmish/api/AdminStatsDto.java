package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;

/// The numbers at the top of the admin console.
@Mapped
public record AdminStatsDto(int players, int online, int liveMatches, int matchesToday, int openFlags, int suspended, int purchasesToday, int gemsSoldToday) {
}
