/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.api;

import com.codename1.annotations.Mapped;

/// A finished run, as the app reports it. The server holds it to `ScoreBounds`.
@Mapped
public record RunDto(String token, String mode, long score, int kills, int gates, int seconds, int peakMultiplier) {
}
