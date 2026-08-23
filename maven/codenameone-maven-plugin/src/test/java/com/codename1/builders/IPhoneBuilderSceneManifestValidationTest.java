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
 * A Catalyst build that asks for windows and supplies its own scene manifest has to be
 * told at build time when that manifest cannot support them. Checking only that the key
 * names appear accepts a manifest that says the opposite of what is needed, and the
 * failure then happens on the device: getWindowManager() reads the bundle, reports
 * unsupported, and the first new Window(...) throws.
 */
class IPhoneBuilderSceneManifestValidationTest {

    @Test
    void aKeySetToFalseIsNotAcceptedAsTrue() {
        assertFalse(IPhoneBuilder.plistKeyIsTrue(
                "<key>UIApplicationSupportsMultipleScenes</key><false/>",
                "UIApplicationSupportsMultipleScenes"),
                "the key is present but says false, which is the case that has to be caught");
    }

    @Test
    void aKeySetToTrueIsAccepted() {
        assertTrue(IPhoneBuilder.plistKeyIsTrue(
                "<key>UIApplicationSupportsMultipleScenes</key>\n    <true/>",
                "UIApplicationSupportsMultipleScenes"));
    }

    @Test
    void aLaterUnrelatedTrueDoesNotVouchForThisKey() {
        // The value of a key is the element that follows it. A <true/> belonging to
        // some other key further down says nothing about this one.
        assertFalse(IPhoneBuilder.plistKeyIsTrue(
                "<key>UIApplicationSupportsMultipleScenes</key><false/>\n"
                        + "<key>UISomethingElse</key><true/>",
                "UIApplicationSupportsMultipleScenes"),
                "a true further down the plist belongs to a different key");
        assertFalse(IPhoneBuilder.plistKeyIsTrue(
                "<key>UIApplicationSupportsMultipleScenes</key>\n"
                        + "<key>UISomethingElse</key><true/>",
                "UIApplicationSupportsMultipleScenes"),
                "another key intervenes, so this one has no true of its own");
    }

    @Test
    void anAbsentKeyIsNotTrue() {
        assertFalse(IPhoneBuilder.plistKeyIsTrue("<key>UIOther</key><true/>",
                "UIApplicationSupportsMultipleScenes"));
    }

    @Test
    void theWindowRoleHasToNameCodenameOnesDelegate() {
        assertTrue(IPhoneBuilder.plistWiresWindowSceneDelegate(
                "<key>UIWindowSceneSessionRoleApplication</key><array><dict>"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string>CodenameOne_GLSceneDelegate</string></dict></array>"));
        assertFalse(IPhoneBuilder.plistWiresWindowSceneDelegate(
                "<key>UIWindowSceneSessionRoleApplication</key><array><dict>"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string>SomeoneElsesSceneDelegate</string></dict></array>"),
                "the role is declared but handed to another delegate, so the secondary "
                        + "scenes a window needs are never adopted");
    }

    @Test
    void anotherRolesDelegateDoesNotCountAsTheWindowRoles() {
        // CarPlay declares its own role and its own delegate. Searching the whole
        // fragment for the delegate name would let CarPlay's configuration vouch for a
        // window role that names nobody.
        assertFalse(IPhoneBuilder.plistWiresWindowSceneDelegate(
                "<key>UIWindowSceneSessionRoleApplication</key><array><dict>"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string>SomeoneElsesSceneDelegate</string></dict></array>"
                        + "<key>CPTemplateApplicationSceneSessionRoleApplication</key><array><dict>"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string>CodenameOne_GLSceneDelegate</string></dict></array>"),
                "the matching delegate belongs to the CarPlay role, not the window role");
    }

    @Test
    void theValidXmlSpellingsOfTrueAreAllAccepted() {
        // <true/> and <true /> and <true></true> are the same element. Rejecting the
        // spaced form would fail a build over valid XML, which is worse than the
        // misconfiguration this check exists to catch.
        for (String spelling : new String[]{"<true/>", "<true />", "<true></true>",
                "\n    <true/>", "<!-- on --><true/>"}) {
            assertTrue(IPhoneBuilder.plistKeyIsTrue(
                    "<key>UIApplicationSupportsMultipleScenes</key>" + spelling,
                    "UIApplicationSupportsMultipleScenes"),
                    "should accept " + spelling);
        }
    }

    @Test
    void theValidXmlSpellingsOfFalseAreAllRejected() {
        for (String spelling : new String[]{"<false/>", "<false />", "<false></false>"}) {
            assertFalse(IPhoneBuilder.plistKeyIsTrue(
                    "<key>UIApplicationSupportsMultipleScenes</key>" + spelling,
                    "UIApplicationSupportsMultipleScenes"),
                    "should reject " + spelling);
        }
    }

    @Test
    void aManifestNamedOnlyInACommentIsNotADeclaredManifest() {
        // These two checks fail the build, so matching a mention rather than a
        // declaration would stop a build that was going to work -- and the builder
        // would skip generating the manifest it should have generated.
        assertFalse(IPhoneBuilder.plistDeclaresKey(
                "<!-- we deliberately do not set UIApplicationSceneManifest here -->",
                "UIApplicationSceneManifest"),
                "a key named inside a comment is not declared");
        assertFalse(IPhoneBuilder.plistDeclaresKey(
                "<key>CFBundleName</key><string>UIApplicationSceneManifest</string>",
                "UIApplicationSceneManifest"),
                "a key quoted as a string value is not declared either");
    }

    @Test
    void aRealDeclarationIsFoundEvenWhenACommentMentionsItFirst() {
        assertTrue(IPhoneBuilder.plistDeclaresKey(
                "<!-- about to set UIApplicationSceneManifest -->\n"
                        + "<key>UIApplicationSceneManifest</key><dict/>",
                "UIApplicationSceneManifest"),
                "the commented mention must not hide the declaration that follows it");
    }

    @Test
    void aCommentedKeyDoesNotVouchForItsValue() {
        assertFalse(IPhoneBuilder.plistKeyIsTrue(
                "<!-- <key>UIApplicationSupportsMultipleScenes</key><true/> -->",
                "UIApplicationSupportsMultipleScenes"),
                "a key and value that exist only inside a comment enable nothing");
    }

    @Test
    void aCommentedOutValueIsNotTheKeysValue() {
        // Found by re-reading the check rather than reported: stepping over a comment's
        // opening and resuming at the next '<' lands inside the comment, so the value
        // someone commented out would be read as the live one -- in the direction that
        // silently enables multi-window on a manifest that disables it.
        assertFalse(IPhoneBuilder.plistKeyIsTrue(
                "<key>UIApplicationSupportsMultipleScenes</key>"
                        + "<!-- <true/> was here --><false/>",
                "UIApplicationSupportsMultipleScenes"),
                "the commented-out true must not stand in for the real false");
        assertTrue(IPhoneBuilder.plistKeyIsTrue(
                "<key>UIApplicationSupportsMultipleScenes</key>"
                        + "<!-- <false/> was here --><true/>",
                "UIApplicationSupportsMultipleScenes"),
                "and the commented-out false must not hide the real true");
    }
}
