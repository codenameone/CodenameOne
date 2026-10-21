package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;

/// A device the server may send push notifications to.
@Mapped
public record PushDeviceDto(String provider, String token, String platform) {
}
