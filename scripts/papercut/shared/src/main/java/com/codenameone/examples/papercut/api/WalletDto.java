/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.api;

import com.codename1.annotations.Mapped;
import java.util.List;

/// The player's ink coins, spare erasers and papers.
@Mapped
public record WalletDto(int inkCoins, int erasers, List<String> owned, String quill) {
}
