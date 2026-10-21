package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;

/// A match being played right now, for the admin console to spectate.
@Mapped
public record LiveMatchDto(String id, String player0, String player1, String name0, String name1, int tick, int crowns0, int crowns1, boolean bot) {
}
