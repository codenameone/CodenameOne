package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;

/// Claiming the reward for a watched rewarded ad.
@Mapped
public record AdRewardDto(String nonce) {
}
