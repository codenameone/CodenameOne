package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;
import java.util.List;

/// A player as they see themselves, and as the admin console sees them. `owned` lists cosmetic item ids; `arena` is the equipped arena skin.
@Mapped
public record PlayerDto(String username, String displayName, int trophies, int wins, int losses, int draws, int coins, int gems, boolean admin, boolean suspended, boolean muted, boolean flagged, List<String> deck, List<String> owned, String arena) {
}
