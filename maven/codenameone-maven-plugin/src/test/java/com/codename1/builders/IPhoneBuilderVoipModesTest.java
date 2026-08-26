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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two exact-match tests behind the VoIP background mode. A substring
 * search over the whole plist fragment passed a build whose UIBackgroundModes
 * carried only remote-notification, because an unrelated value elsewhere --
 * a URL scheme, a display name -- happened to contain the four letters. The
 * app then shipped registered for VoIP pushes and unable to be woken by one.
 */
class IPhoneBuilderVoipModesTest {

    private static final String MODES_HEAD =
            "<key>UIBackgroundModes</key>\n<array>\n";

    @Test
    void voipIsFoundInsideTheBackgroundModesArray() {
        assertTrue(IPhoneBuilder.injectedModesIncludeVoip(
                MODES_HEAD + "<string>audio</string>\n"
                + "<string>voip</string>\n</array>"));
    }

    @Test
    void voipElsewhereInThePlistIsNotTheBackgroundMode() {
        String fragment = "<key>CFBundleURLSchemes</key>\n<array>\n"
                + "<string>myvoipapp</string>\n</array>\n"
                + MODES_HEAD + "<string>remote-notification</string>\n"
                + "</array>";
        assertFalse(IPhoneBuilder.injectedModesIncludeVoip(fragment),
                "a URL scheme that merely contains \"voip\" is not a"
                        + " background mode");
    }

    @Test
    void aValueThatMerelyContainsVoipIsNotVoip() {
        assertFalse(IPhoneBuilder.injectedModesIncludeVoip(
                MODES_HEAD + "<string>voip-ish</string>\n</array>"));
    }

    @Test
    void aSecondModesKeyIsStillSearched() {
        // Two UIBackgroundModes keys are a malformed plist, but reading only
        // the first would answer for the wrong array.
        assertTrue(IPhoneBuilder.injectedModesIncludeVoip(
                MODES_HEAD + "<string>audio</string>\n</array>\n"
                + MODES_HEAD + "<string>voip</string>\n</array>"));
    }

    @Test
    void aModeIsMatchedWholeInTheHint() {
        assertTrue(IPhoneBuilder.listedModes("audio,voip").contains("voip"));
        assertTrue(IPhoneBuilder.listedModes("audio voip").contains("voip"));
        assertFalse(IPhoneBuilder.listedModes("remote-notification")
                .contains("voip"));
        assertFalse(IPhoneBuilder.listedModes("myvoipmode").contains("voip"),
                "a mode whose name contains voip is not voip");
    }
}
