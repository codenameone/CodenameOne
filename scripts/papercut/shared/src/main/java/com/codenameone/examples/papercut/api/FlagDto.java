/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.api;

import com.codename1.annotations.Mapped;

/// A run worth a person's look: past `ScoreBounds#suspicious`, or a player reported.
@Mapped
public record FlagDto(String id, String username, String displayName, String mode, long score, int kills, int seconds, String reason, long createdAt, boolean open) {
}
