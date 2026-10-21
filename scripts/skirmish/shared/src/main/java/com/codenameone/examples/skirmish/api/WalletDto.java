package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;
import java.util.List;

/// What a player holds.
@Mapped
public record WalletDto(int coins, int gems, List<String> owned, String arena, int adRewardsLeft) {
}
