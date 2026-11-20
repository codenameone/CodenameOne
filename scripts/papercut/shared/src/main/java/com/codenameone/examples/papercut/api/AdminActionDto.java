/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.api;

import com.codename1.annotations.Mapped;

/// What an admin does to a player: WARN, SUSPEND (for `days`), BAN, UNBAN or RESET_SCORES.
@Mapped
public record AdminActionDto(String action, String reason, int days) {
}
