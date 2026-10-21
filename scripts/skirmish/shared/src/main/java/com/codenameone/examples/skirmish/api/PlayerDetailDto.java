package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;
import java.util.List;

/// Everything the admin console shows about one player.
@Mapped
public record PlayerDetailDto(PlayerDto player, List<MatchSummaryDto> matches, List<FlagDto> flags, List<AuditDto> actions, List<String> purchases) {
}
