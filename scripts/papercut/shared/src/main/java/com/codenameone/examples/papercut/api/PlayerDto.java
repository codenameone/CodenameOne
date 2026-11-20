/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.api;

import com.codename1.annotations.Mapped;
import java.util.List;

/// A player as they see themselves, and as the console sees them. The bests are per mode;
/// `quill` is the equipped paper and `owned` every paper they have.
@Mapped
public record PlayerDto(String username, String displayName, int inkCoins, long bestClassic, long bestFold, long bestDaily, int runs, boolean admin, boolean suspended, boolean flagged, String quill, List<String> owned) {
}
