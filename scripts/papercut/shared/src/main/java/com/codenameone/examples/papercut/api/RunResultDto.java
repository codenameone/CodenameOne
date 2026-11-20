/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.api;

import com.codename1.annotations.Mapped;

/// What became of a submitted run. `rank` is on the all-time board of its mode, 0 when
/// it was refused; `coinsEarned` went into the wallet.
@Mapped
public record RunResultDto(boolean accepted, boolean flagged, boolean newBest, long best, int rank, int coinsEarned, String message) {
}
