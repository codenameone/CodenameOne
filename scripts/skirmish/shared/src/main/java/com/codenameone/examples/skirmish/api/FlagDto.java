package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;

/// A reason to look at a player: a report from an opponent, or something the server noticed itself (`kind` AUTO).
@Mapped
public record FlagDto(String id, String username, String displayName, String kind, String reason, String matchId, String reporter, long createdAt, boolean open) {
}
