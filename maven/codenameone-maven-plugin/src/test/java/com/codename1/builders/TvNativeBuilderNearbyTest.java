/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tvOS slice inherits the iOS link phase, so a framework the iOS slice
 * links for {@code com.codename1.nearby} has to be weak-linked here or the
 * tvOS archive fails while resolving it.
 *
 * <p>Which ones is a measured fact, not a symmetry with the watch list: on the
 * Xcode 26.3 SDKs tvOS has no NearbyInteraction and no AccessorySetupKit, and
 * does ship MultipeerConnectivity. Weak-linking the one it has would only
 * obscure that, so this pins both halves of the distinction.</p>
 */
class TvNativeBuilderNearbyTest {

    private static String optionalFrameworks() throws Exception {
        Field f = TvNativeBuilder.class
                .getDeclaredField("TV_OPTIONAL_FRAMEWORKS");
        f.setAccessible(true);
        return (String) f.get(null);
    }

    @Test
    void theFrameworksTvosLacksAreWeakLinked() throws Exception {
        String list = optionalFrameworks();
        assertTrue(list.contains("NearbyInteraction.framework"), list);
        assertTrue(list.contains("AccessorySetupKit.framework"), list);
    }

    @Test
    void theFrameworkTvosShipsIsNotWeakLinked() throws Exception {
        String list = optionalFrameworks();
        assertFalse(list.contains("MultipeerConnectivity.framework"), list);
    }
}
