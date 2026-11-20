/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.api;

import com.codename1.annotations.Mapped;

/// A push notification to every player, or to those not seen for `days`. `targets` is how
/// many it reached.
@Mapped
public record BroadcastDto(String id, String title, String body, int days, int targets, long createdAt) {
}
