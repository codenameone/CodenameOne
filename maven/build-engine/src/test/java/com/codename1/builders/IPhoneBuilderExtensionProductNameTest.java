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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// The generated Call Directory target's product name, which the host's
/// "Embed App Extensions" phase has to reference by the name Xcode will
/// actually build.
///
/// The phase used the TARGET name unconditionally while
/// ios.call.directory.buildSettings.PRODUCT_NAME could rename the product, so
/// an override had the host embedding a file that was never built and the
/// archive failing on a name the developer never wrote.
class IPhoneBuilderExtensionProductNameTest {

    private static final String TARGET = "CN1CallDirectory";

    @Test
    void theDefaultProductIsTheTarget() {
        // What the builder sets before any override, and the case the
        // hardcoded reference was right for.
        assertEquals(TARGET, IPhoneBuilder.effectiveExtensionProductName(
                "$(TARGET_NAME)", TARGET));
        assertEquals(TARGET, IPhoneBuilder.effectiveExtensionProductName(
                "${TARGET_NAME}", TARGET));
        assertEquals(TARGET,
                IPhoneBuilder.effectiveExtensionProductName(null, TARGET));
        assertEquals(TARGET,
                IPhoneBuilder.effectiveExtensionProductName("   ", TARGET));
    }

    @Test
    void aLiteralOverrideIsHonoured() {
        // Honoured rather than refused: a literal IS the product's name, so
        // the embed phase can name it and the hint does what it says.
        assertEquals("Directory", IPhoneBuilder.effectiveExtensionProductName(
                "Directory", TARGET));
        assertEquals("Directory", IPhoneBuilder.effectiveExtensionProductName(
                "  Directory  ", TARGET));
    }

    @Test
    void anUnevaluableOverrideIsRefusedRatherThanGuessed() {
        // $(CONFIGURATION)-Directory is a name only Xcode can work out.
        // Passing the unexpanded text through would produce the same
        // missing-product failure it is meant to prevent, with the hint that
        // caused it invisible -- so the builder throws instead, naming it.
        assertNull(IPhoneBuilder.effectiveExtensionProductName(
                "$(CONFIGURATION)-Directory", TARGET));
        assertNull(IPhoneBuilder.effectiveExtensionProductName(
                "${CONFIGURATION}-Directory", TARGET));
    }
}
