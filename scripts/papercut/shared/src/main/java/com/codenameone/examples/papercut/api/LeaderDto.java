/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.api;

import com.codename1.annotations.Mapped;

/// One row of a leaderboard. `me` marks the player asking.
@Mapped
public record LeaderDto(int rank, String username, String displayName, long score, String quill, boolean me) {
}
