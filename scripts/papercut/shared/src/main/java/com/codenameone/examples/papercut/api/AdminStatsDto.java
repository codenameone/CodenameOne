/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.api;

import com.codename1.annotations.Mapped;

/// The console's front page.
@Mapped
public record AdminStatsDto(int players, int activeToday, int runsToday, int flagsOpen, long topScore, long dailySeed, int purchases) {
}
