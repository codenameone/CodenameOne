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

    /// The builder's own source, for a rule that lives inside a method with
    /// no seam to call -- the same way the scanner parity tests do it.
    private static String source() throws Exception {
        java.io.File f = new java.io.File(
                "src/main/java/com/codename1/builders/TvNativeBuilder.java");
        assertTrue(f.exists(), "builder source must be readable: "
                + f.getAbsolutePath());
        return new String(java.nio.file.Files.readAllBytes(f.toPath()),
                java.nio.charset.StandardCharsets.UTF_8);
    }

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

    /**
     * The tvOS plist carries the local-network keys the transport needs.
     *
     * <p>MultipeerConnectivity ships on tvOS and is deliberately linked for
     * this slice, but tvOS 14 gates local-network discovery on the same two
     * declarations iOS does -- and the tvOS plist is generated separately,
     * carrying only bundle metadata, capabilities and fonts. So the
     * framework was there, the native transport was compiled in, and the
     * target could neither advertise nor browse.</p>
     */
    @Test
    void theTvPlistCarriesTheLocalNetworkKeys() throws Exception {
        String src = source();
        assertTrue(src.contains("NSBonjourServices"),
                "the tvOS plist has to declare the Bonjour services the"
                + " iOS slice resolved");
        assertTrue(src.contains("NSLocalNetworkUsageDescription"),
                "and the usage description, without which tvOS 14 refuses"
                + " the discovery outright");
    }

    /**
     * The generated hint is read, not only a hand-written plistInject.
     *
     * <p>The build writes to whichever source the app left it: an app that
     * declares NSBonjourServices itself puts it in {@code ios.plistInject}
     * and the merge leaves it alone, while every other build -- the
     * ordinary generated one -- gets a comma-separated
     * {@code ios.NSBonjourServices} instead. Reading only the first found
     * nothing in the normal case, so the tvOS plist was written without
     * either local-network key and the slice still could not discover
     * anything.</p>
     */
    @Test
    void theTvPlistReadsTheGeneratedBonjourHint() throws Exception {
        String src = source();
        assertTrue(src.contains("ios.NSBonjourServices"),
                "the tvOS plist has to read the hint the nearby merge"
                + " writes, not only ios.plistInject");
    }
}
