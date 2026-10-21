package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;

/// A player reporting their opponent after a match.
@Mapped
public record ReportDto(String matchId, String reason) {
}
