package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;

/// One line of a player's match history. `result` is 1 for a win, 0 for a draw and -1 for a loss.
@Mapped
public record MatchSummaryDto(String id, String opponent, String opponentName, int result, int myCrowns, int theirCrowns, int trophyChange, long endedAt, int ticks) {
}
