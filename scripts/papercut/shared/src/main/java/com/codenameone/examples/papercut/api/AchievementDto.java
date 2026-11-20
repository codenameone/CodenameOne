/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.api;

import com.codename1.annotations.Mapped;

/// A goal, and how far the player is toward it.
@Mapped
public record AchievementDto(String id, String name, String description, boolean unlocked, long progress, long goal) {
}
