/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.api;

import com.codename1.annotations.Mapped;

/// Today's sheet: the date, the seed every player is dealt, the asker's best and the day's top.
@Mapped
public record DailyDto(String date, long seed, long myBest, long topScore, String topName, int players) {
}
