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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two exact-match tests behind the VoIP background mode. A substring
 * search over the whole plist fragment passed a build whose UIBackgroundModes
 * carried only remote-notification, because an unrelated value elsewhere --
 * a URL scheme, a display name -- happened to contain the four letters. The
 * app then shipped registered for VoIP pushes and unable to be woken by one.
 */
class IPhoneBuilderVoipModesTest {

    private static final String BUILDER_SOURCE =
            "src/main/java/com/codename1/builders/IPhoneBuilder.java";

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

    @Test
    void anEmptyBundleIdOverrideKeepsTheDefault() throws Exception {
        // A present-but-empty override is not an override. Left in, it
        // replaced the resolved identifier with nothing: a simulator target
        // with no bundle id and a host plist still naming the default.
        String src = builderSource();
        int loop = src.indexOf(
                "key.startsWith(\"ios.call.directory.buildSettings.\")");
        assertTrue(loop > 0, "the override loop has to exist");
        String after = src.substring(loop, Math.min(src.length(), loop + 700));
        assertTrue(after.contains("trim().length() == 0"),
                "an empty override has to be skipped: " + after);
        assertTrue(after.contains("continue;"), after);
    }

    @Test
    void theInjectedKeyIsMatchedExactly() throws Exception {
        // A project with an unrelated MyCN1CallAppGroupSetting matched a bare
        // substring test, which suppressed generation entirely -- so the host
        // plist carried neither the group nor the extension identifier.
        String src = builderSource();
        // The live fragment, and the exact key within it.
        assertTrue(src.contains("plistWithoutComments(inject)"),
                "appendCallPlist has to test the fragment without comments");
        // The PROPERTY, not the spelling that used to provide it. This
        // pinned .contains("<key>" + key + "</key>") until that literal
        // turned out to miss "<key >CN1CallAppGroup</key >" -- so the
        // assertion was holding the implementation in place rather than the
        // behaviour, and had to be edited to allow the fix.
        assertTrue(src.contains("plistKeyNamed(plistWithoutComments(inject)"),
                "appendCallPlist has to find the key as an ELEMENT, so a"
                + " project that spaced its own tags is still seen to"
                + " declare it");
    }

    @Test
    void aCommentedOutDeclarationIsNotSupplied() {
        // Every "does the project already supply this?" test used to answer
        // yes for a declaration the project had COMMENTED OUT. The plist
        // parser drops the comment, so the builder then stood aside for a key
        // that was not there -- an app registered for VoIP pushes with no
        // live background mode to wake it, and a host plist with no app group
        // for the call directory to read. Both fail only on a device.
        String commented = "<!-- <key>UIBackgroundModes</key>"
                + "<array><string>voip</string></array> -->";
        assertEquals("", IPhoneBuilder.plistWithoutComments(commented).trim());
        assertFalse(IPhoneBuilder.injectedModesIncludeVoip(
                IPhoneBuilder.plistWithoutComments(commented)));

        // A live declaration after a comment is still live.
        String live = "<!-- old -->\n<key>UIBackgroundModes</key>"
                + "<array><string>voip</string></array>";
        assertTrue(IPhoneBuilder.injectedModesIncludeVoip(
                IPhoneBuilder.plistWithoutComments(live)));

        // An unterminated comment swallows the rest, as a parser would.
        assertEquals("<key>A</key>", IPhoneBuilder.plistWithoutComments(
                "<key>A</key><!-- <key>B</key>"));
        assertNull(IPhoneBuilder.plistWithoutComments(null));
    }

    @Test
    public void xmlSpacingInsideTheTagsIsStillXml() {
        // "<array >" and "<string >voip</string >" are valid XML and only
        // the SERIALIZER writes the compact spelling, so a literal
        // comparison rejected a fragment that already declared the mode --
        // and the build then refused a configuration that was correct,
        // which is a worse answer than the one the literal test replaced.
        assertTrue(IPhoneBuilder.injectedModesIncludeVoip(
                "<key>UIBackgroundModes</key><array ><string >voip</string >"
                + "</array>"),
                "spacing inside the tags does not change what they say");
        assertTrue(IPhoneBuilder.injectedModesIncludeVoip(
                "<key>UIBackgroundModes</key>\n<array>\n"
                + "  <string>remote-notification</string>\n"
                + "  <string>voip</string>\n</array>"),
                "and neither does formatting between them");
        // Still NOT matched where it does not belong: the mode has to be in
        // the array this key names.
        assertFalse(IPhoneBuilder.injectedModesIncludeVoip(
                "<key>UIBackgroundModes</key><string >audio</string >"
                + "<key>Other</key><array ><string >voip</string ></array>"),
                "a later array is not this key's value");
        assertFalse(IPhoneBuilder.injectedModesIncludeVoip(
                "<key>UIBackgroundModes</key><array ><string >myvoipapp"
                + "</string ></array>"),
                "and a longer mode that contains it is not it");
    }

    @Test
    public void aSpacedKeyIsStillTheKey() {
        // "<key >UIBackgroundModes</key >" is the same plist, and every
        // decision downstream is about whether the PROJECT supplies
        // something -- so reading its declaration as absent is the direction
        // that hurts: the build generates its own array beside the project's
        // and plist assembly refuses a fragment that declares the key twice.
        assertTrue(IPhoneBuilder.injectedModesIncludeVoip(
                "<key >UIBackgroundModes</key ><array><string>voip</string>"
                + "</array>"),
                "spacing in the key does not change which key it is");
        assertFalse(IPhoneBuilder.injectedModesIncludeVoip(
                "<key >UIBackgroundModesExtra</key ><array><string>voip"
                + "</string></array>"),
                "and a longer key that starts with ours is not ours");
    }

    @Test
    public void theAssemblyReadsTheKeyTheSameWayTheVoipBranchDoes()
            throws Exception {
        // Two readers of one fragment. The VoIP branch strips comments and
        // finds the key structurally; the final assembly used a raw
        // substring test -- so a project that had COMMENTED OUT an old
        // UIBackgroundModes had ios.background_modes set for it by the first
        // and was then refused by the second for using both mechanisms. The
        // plist parser sees what the stripped read sees.
        String src = builderSource();
        int at = src.indexOf("if (backgroundModesStr != null) {");
        assertTrue(at >= 0, "the assembly block has to be findable");
        String block = src.substring(at, src.indexOf("</array>", at));
        assertFalse(block.contains("inject.contains(\"UIBackgroundModes\")"),
                "the assembly must not test the raw fragment: " + block);
        assertTrue(block.contains("plistKeyNamed(plistWithoutComments(inject)"),
                "it reads the live key, as the VoIP branch does");
    }

    private static String builderSource() throws Exception {
        java.io.File f = new java.io.File(BUILDER_SOURCE);
        assertTrue(f.exists(), "builder source must be readable: "
                + f.getAbsolutePath());
        return new String(java.nio.file.Files.readAllBytes(f.toPath()),
                java.nio.charset.StandardCharsets.UTF_8);
    }
}
