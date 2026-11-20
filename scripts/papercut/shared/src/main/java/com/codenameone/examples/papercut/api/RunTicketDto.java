/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.api;

import com.codename1.annotations.Mapped;

/// Permission to submit one run: the token the result must carry, and for the daily the seed
/// every player is dealt that day.
@Mapped
public record RunTicketDto(String token, String mode, long seed, long expiresAt) {
}
