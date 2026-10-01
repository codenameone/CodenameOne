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

/// Reading the value an `ios.plistInject` fragment gives a key.
///
/// A plist key is followed by exactly one value element. The reader used to
/// search forward for the next `<string>` anywhere in the fragment, so a key
/// given `<false/>` was reported either as absent or as holding a LATER key's
/// value -- and for CN1CallAppGroup, whose value the generated Call Directory
/// extension resolves too, that meant the conflict check compared this key
/// against somebody else's string, and the host plist shipped a boolean where
/// the App Group belonged.
class IPhoneBuilderCallPlistValueTypeTest {

    @Test
    void aStringValueIsReadAsAString() {
        String inject = "<key>CN1CallAppGroup</key><string>group.a</string>";
        assertEquals("string", IPhoneBuilder.injectedPlistValueTag(
                inject, "CN1CallAppGroup"));
    }

    @Test
    void aNonStringValueIsReportedAsItsOwnType() {
        // Not "absent". <false/> IS a value; it is simply the wrong one, and
        // reading no-string-here as no-value-here is what let it through.
        assertEquals("false", IPhoneBuilder.injectedPlistValueTag(
                "<key>CN1CallAppGroup</key><false/>", "CN1CallAppGroup"));
        assertEquals("array", IPhoneBuilder.injectedPlistValueTag(
                "<key>CN1CallAppGroup</key><array><string>a</string></array>",
                "CN1CallAppGroup"));
        assertEquals("dict", IPhoneBuilder.injectedPlistValueTag(
                "<key>CN1CallAppGroup</key>\n  <dict/>", "CN1CallAppGroup"));
    }

    @Test
    void aLaterKeysStringIsNotThisKeysValue() {
        // The failure the forward scan produced: the group is a boolean and
        // the string belongs to a different key entirely.
        String inject = "<key>CN1CallAppGroup</key><false/>\n"
                + "<key>CN1CallProviderName</key><string>Acme</string>";
        assertEquals("false", IPhoneBuilder.injectedPlistValueTag(
                inject, "CN1CallAppGroup"));
    }

    @Test
    void theSelfClosingEmptyStringIsStillAString() {
        // Both spellings. <string /> is a valid empty value, and the reader
        // that hunted for the next "<string>" stepped straight over it --
        // returning a LATER key's text, which was the same defect one
        // spelling further on.
        assertEquals("string", IPhoneBuilder.injectedPlistValueTag(
                "<key>CN1CallAppGroup</key><string/>", "CN1CallAppGroup"));
        assertEquals("string", IPhoneBuilder.injectedPlistValueTag(
                "<key>CN1CallAppGroup</key><string />", "CN1CallAppGroup"));
    }

    @Test
    void aKeyThatIsNotThereHasNoValue() {
        assertNull(IPhoneBuilder.injectedPlistValueTag(
                "<key>Other</key><string>x</string>", "CN1CallAppGroup"));
        assertNull(IPhoneBuilder.injectedPlistValueTag(
                "", "CN1CallAppGroup"));
    }

    @Test
    void aCommentedKeyDoesNotSupplyAValue() {
        assertNull(IPhoneBuilder.injectedPlistValueTag(
                "<!-- <key>CN1CallAppGroup</key><false/> -->",
                "CN1CallAppGroup"));
    }
}
