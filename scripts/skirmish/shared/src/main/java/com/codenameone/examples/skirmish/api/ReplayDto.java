package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;

/// A recorded match. `replay` is the shared rules' own encoding: a seed, two decks and the commands, nothing else.
@Mapped
public record ReplayDto(String id, String player0, String player1, String name0, String name1, int result, int crowns0, int crowns1, String replay, String rulesVersion, long endedAt) {
}
