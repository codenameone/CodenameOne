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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/// The builder removes the template's phone-only orientation list by exact text
/// when ios.interface_orientation is set. If the template drifts, that removal
/// silently matches nothing and the hint stops reaching iPhones.
class IPhoneBuilderOrientationTemplateTest {
    @Test
    void phoneOrientationListMatchesTheTemplateAndOmitsUpsideDown() throws Exception {
        String plist = new String(Files.readAllBytes(Paths.get("..", "..", "vm", "ByteCodeTranslator", "src",
                "template", "template", "template-Info.plist")), StandardCharsets.UTF_8);
        int first = plist.indexOf(IPhoneBuilder.IPHONE_DEFAULT_ORIENTATIONS);
        assertEquals(true, first >= 0, "template no longer contains IPHONE_DEFAULT_ORIENTATIONS verbatim");
        assertEquals(first, plist.lastIndexOf(IPhoneBuilder.IPHONE_DEFAULT_ORIENTATIONS));
        assertFalse(IPhoneBuilder.IPHONE_DEFAULT_ORIENTATIONS.contains("UpsideDown"));
    }
}
