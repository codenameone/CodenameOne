package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;

/// What an admin does to a player: WARN, MUTE, UNMUTE, SUSPEND (for `days`), BAN, UNBAN, RESET_TROPHIES or REVOKE_ITEMS.
@Mapped
public record AdminActionDto(String action, String reason, int days) {
}
