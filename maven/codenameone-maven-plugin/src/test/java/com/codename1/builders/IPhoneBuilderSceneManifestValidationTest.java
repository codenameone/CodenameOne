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

    private static final String WINDOW_ROLE =
            "        <key>UIWindowSceneSessionRoleApplication</key>\n"
            + "        <array>\n"
            + "            <dict>\n"
            + "                <key>UISceneDelegateClassName</key>\n"
            + "                <string>CodenameOne_GLSceneDelegate</string>\n"
            + "            </dict>\n"
            + "        </array>\n";

    private static String manifest(String body) {
        return "<key>UIApplicationSceneManifest</key>\n<dict>\n" + body + "</dict>";
    }

    /// The root dictionary's body of a whole document, which is the level the scene
    /// manifest is a member of. The validators take a fragment at that level, so a
    /// document has to be unwrapped before they can answer about it -- which is itself
    /// the point of the manifest having to be a root member.
    private static String rootBody(String document) {
        int open = document.indexOf("<dict>");
        int close = document.lastIndexOf("</dict>");
        return document.substring(open + "<dict>".length(), close);
    }

    /// A whole plist document, which is what the Mac-slice transform is handed: it
    /// reads the root dictionary, and a bare fragment has none.
    private static String document(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<plist version=\"1.0\">\n<dict>\n"
                + "    <key>CFBundleName</key>\n    <string>Demo</string>\n"
                + body
                + "</dict>\n</plist>\n";
    }

    private static String wellFormedManifest() {
        return manifest(
                "    <key>UIApplicationSupportsMultipleScenes</key>\n    <true/>\n"
                + "    <key>UISceneConfigurations</key>\n    <dict>\n"
                + WINDOW_ROLE
                + "    </dict>\n");
    }

    @Test
    void aWellFormedManifestSatisfiesBothQuestions() {
        String plist = wellFormedManifest();
        assertTrue(IPhoneBuilder.plistManifestSupportsMultipleScenes(plist));
        assertTrue(IPhoneBuilder.plistManifestWiresWindowScene(plist));
    }

    @Test
    void theSupportKeyHasToBeAMemberOfTheManifestNotBuriedInIt() {
        // The key is inside an unrelated metadata dictionary nested in the manifest.
        // UIKit reads members of the manifest dictionary, so it ignores this one and
        // the app has no multi-scene support -- but a search that only asks whether
        // the key appears anywhere in the manifest says yes.
        String plist = manifest(
                "    <key>CN1Metadata</key>\n    <dict>\n"
                + "        <key>UIApplicationSupportsMultipleScenes</key>\n"
                + "        <true/>\n"
                + "    </dict>\n"
                + "    <key>UISceneConfigurations</key>\n    <dict>\n"
                + WINDOW_ROLE
                + "    </dict>\n");
        assertFalse(IPhoneBuilder.plistManifestSupportsMultipleScenes(plist),
                "a key nested in another dictionary is not a member of the manifest");
        // And this is exactly what the unscoped question would have answered.
        assertTrue(IPhoneBuilder.plistKeyIsTrue(
                        IPhoneBuilder.plistManifestScope(plist),
                        "UIApplicationSupportsMultipleScenes"),
                "the search-anywhere question is what accepted it");
    }

    @Test
    void theWindowRoleHasToBeAMemberOfUISceneConfigurations() {
        // The role sits in an unrelated dictionary rather than under
        // UISceneConfigurations, so UIKit has no configuration to create a window with.
        String plist = manifest(
                "    <key>UIApplicationSupportsMultipleScenes</key>\n    <true/>\n"
                + "    <key>CN1Metadata</key>\n    <dict>\n"
                + WINDOW_ROLE
                + "    </dict>\n"
                + "    <key>UISceneConfigurations</key>\n    <dict>\n    </dict>\n");
        assertFalse(IPhoneBuilder.plistManifestWiresWindowScene(plist),
                "a role outside UISceneConfigurations configures nothing");
        assertTrue(IPhoneBuilder.plistWiresWindowSceneDelegate(
                        IPhoneBuilder.plistManifestScope(plist)),
                "the search-anywhere question is what accepted it");
    }

    @Test
    void aManifestWithNoSceneConfigurationsAtAllWiresNothing() {
        String plist = manifest(
                "    <key>UIApplicationSupportsMultipleScenes</key>\n    <true/>\n");
        assertTrue(IPhoneBuilder.plistManifestSupportsMultipleScenes(plist));
        assertFalse(IPhoneBuilder.plistManifestWiresWindowScene(plist));
    }

    @Test
    void aCommentedMemberIsNotAMember() {
        String plist = manifest(
                "    <!-- <key>UIApplicationSupportsMultipleScenes</key><true/> -->\n"
                + "    <key>UISceneConfigurations</key>\n    <dict>\n"
                + WINDOW_ROLE
                + "    </dict>\n");
        assertFalse(IPhoneBuilder.plistManifestSupportsMultipleScenes(plist));
    }

    @Test
    void whitespaceInTheContainerTagsDoesNotHideMembership() {
        String plist = wellFormedManifest()
                .replace("</key>", "</key >")
                .replace("<dict>", "<dict >");
        assertTrue(IPhoneBuilder.plistManifestSupportsMultipleScenes(plist));
        assertTrue(IPhoneBuilder.plistManifestWiresWindowScene(plist));
    }

    @Test
    void theMacSliceGetsMultipleScenesAndTheSharedPlistDoesNot() {
        // One Info.plist serves both destinations of one target, so the shared file
        // stays false and the Mac copy is what differs. Getting this backwards opts
        // every iPad build into multi-window behaviour it never asked for.
        String shared = document(manifest(
                "    <key>UIApplicationSupportsMultipleScenes</key>\n    <false/>\n"
                + "    <key>UISceneConfigurations</key>\n    <dict>\n"
                + WINDOW_ROLE
                + "    </dict>\n"));
        String mac = IPhoneBuilder.plistForMacSlice(shared);
        assertFalse(IPhoneBuilder.plistManifestSupportsMultipleScenes(rootBody(shared)),
                "the shared plist the iOS slice reads must stay false");
        assertTrue(IPhoneBuilder.plistManifestSupportsMultipleScenes(rootBody(mac)),
                "the Mac slice's copy must say true");
        assertTrue(IPhoneBuilder.plistManifestWiresWindowScene(rootBody(mac)),
                "and must keep the scene configuration");
    }

    @Test
    void theFlipTouchesOnlyTheManifestsOwnMember() {
        // A key of the same name in an unrelated dictionary is somebody else's, and
        // rewriting it would change a setting the application chose.
        String unrelated = "    <key>CN1Metadata</key>\n    <dict>\n"
                + "        <key>UIApplicationSupportsMultipleScenes</key>\n        <false/>\n"
                + "    </dict>\n";
        String plist = document(unrelated
                + manifest("    <key>UIApplicationSupportsMultipleScenes</key>\n    <false/>\n"));
        String mac = IPhoneBuilder.plistForMacSlice(plist);
        assertTrue(IPhoneBuilder.plistManifestSupportsMultipleScenes(
                        rootBody(mac)),
                "the manifest's own member is what gets set");
        assertTrue(mac.contains(unrelated),
                "the unrelated dictionary is left exactly as it was");
    }

    @Test
    void aPlistWithNoSceneManifestIsReturnedUnchanged() {
        String plist = "<key>CFBundleName</key><string>x</string>";
        assertTrue(plist.equals(IPhoneBuilder.plistForMacSlice(plist)));
    }

    @Test
    void theFlipIsIdempotent() {
        String once = IPhoneBuilder.plistForMacSlice(document(wellFormedManifest()));
        assertTrue(once.equals(IPhoneBuilder.plistForMacSlice(once)),
                "a manifest that already says true needs no second copy");
    }

    @Test
    void aPlistWithNoManifestGetsAWholeOneForTheMacSlice() {
        // The default Catalyst build: ios.uiscene is off and there is no CarPlay, so
        // the shared plist carries no manifest at all -- declaring one there would
        // activate the UIScene lifecycle for the iPhone/iPad artifact while it still
        // carries its main NIB, which FrontBoard terminates at launch. The manifest
        // has to appear only in the Mac slice's copy.
        String shared = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<plist version=\"1.0\">\n<dict>\n"
                + "    <key>CFBundleName</key>\n    <string>Demo</string>\n"
                + "</dict>\n</plist>\n";
        assertFalse(IPhoneBuilder.plistDeclaresKey(shared, "UIApplicationSceneManifest"),
                "the shared plist must stay free of a scene manifest");
        String mac = IPhoneBuilder.plistForMacSlice(shared);
        assertTrue(IPhoneBuilder.plistDeclaresKey(mac, "UIApplicationSceneManifest"),
                "the Mac copy is where the manifest appears");
        assertTrue(IPhoneBuilder.plistManifestSupportsMultipleScenes(rootBody(mac)));
        assertTrue(IPhoneBuilder.plistManifestWiresWindowScene(rootBody(mac)));
        assertTrue(mac.contains("<key>CFBundleName</key>"),
                "and everything the build already put there is kept");
        assertTrue(mac.trim().endsWith("</plist>"),
                "the manifest goes inside the root dictionary, not after it");
    }

    @Test
    void theWindowRoleHasToBeAnArrayOfConfigurations() {
        // A role written as a dictionary rather than an array of configuration
        // dictionaries describes no window UIKit can create.
        String plist = manifest(
                "    <key>UIApplicationSupportsMultipleScenes</key>\n    <true/>\n"
                + "    <key>UISceneConfigurations</key>\n    <dict>\n"
                + "        <key>UIWindowSceneSessionRoleApplication</key>\n"
                + "        <dict>\n"
                + "            <key>UISceneDelegateClassName</key>\n"
                + "            <string>CodenameOne_GLSceneDelegate</string>\n"
                + "        </dict>\n"
                + "    </dict>\n");
        assertFalse(IPhoneBuilder.plistManifestWiresWindowScene(plist),
                "the role's value has to be an array of configurations");
    }

    @Test
    void theDelegateHasToBeOwnedByAConfigurationNotBuriedUnderIt() {
        // The delegate key sits in a metadata dictionary inside the configuration, so
        // the configuration itself names no delegate.
        String plist = manifest(
                "    <key>UIApplicationSupportsMultipleScenes</key>\n    <true/>\n"
                + "    <key>UISceneConfigurations</key>\n    <dict>\n"
                + "        <key>UIWindowSceneSessionRoleApplication</key>\n"
                + "        <array>\n"
                + "            <dict>\n"
                + "                <key>CN1Metadata</key>\n"
                + "                <dict>\n"
                + "                    <key>UISceneDelegateClassName</key>\n"
                + "                    <string>CodenameOne_GLSceneDelegate</string>\n"
                + "                </dict>\n"
                + "            </dict>\n"
                + "        </array>\n"
                + "    </dict>\n");
        assertFalse(IPhoneBuilder.plistManifestWiresWindowScene(plist),
                "a configuration that does not itself name the delegate wires nothing");
    }

    @Test
    void oneValidConfigurationAmongSeveralIsEnough() {
        String plist = manifest(
                "    <key>UIApplicationSupportsMultipleScenes</key>\n    <true/>\n"
                + "    <key>UISceneConfigurations</key>\n    <dict>\n"
                + "        <key>UIWindowSceneSessionRoleApplication</key>\n"
                + "        <array>\n"
                + "            <dict>\n"
                + "                <key>UISceneDelegateClassName</key>\n"
                + "                <string>SomebodyElse</string>\n"
                + "            </dict>\n"
                + "            <dict>\n"
                + "                <key>UISceneDelegateClassName</key>\n"
                + "                <string>CodenameOne_GLSceneDelegate</string>\n"
                + "            </dict>\n"
                + "        </array>\n"
                + "    </dict>\n");
        assertTrue(IPhoneBuilder.plistManifestWiresWindowScene(plist));
    }

    @Test
    void anArraysMembersAreItsOwnElements() {
        java.util.List<String> members = IPhoneBuilder.plistArrayMembers(
                "<array>\n  <string>a</string>\n  <array>\n    <string>b</string>\n"
                + "  </array>\n  <dict>\n    <key>k</key><string>v</string>\n  </dict>\n"
                + "</array>");
        assertEquals(3, members.size(), "a nested array is one member, not its contents");
        assertTrue(members.get(1).contains("<string>b</string>"));
        assertTrue(members.get(2).startsWith("<dict>"));
    }

    @Test
    void aCarPlayOnlyManifestGainsTheWindowRoleOnTheMacSlice() {
        // macNative with CarPlay and ios.uiscene off: the build emits a manifest for
        // CarPlay's sake, and it carries only the CarPlay role. Flipping the support
        // key is not enough -- the Catalyst bundle would say multiple scenes are
        // supported and describe no configuration to create a window from.
        String carPlayRole =
                "        <key>CPTemplateApplicationSceneSessionRoleApplication</key>\n"
                + "        <array>\n"
                + "            <dict>\n"
                + "                <key>UISceneDelegateClassName</key>\n"
                + "                <string>CodenameOne_CarPlaySceneDelegate</string>\n"
                + "            </dict>\n"
                + "        </array>\n";
        String shared = document(manifest(
                "    <key>UIApplicationSupportsMultipleScenes</key>\n    <false/>\n"
                + "    <key>UISceneConfigurations</key>\n    <dict>\n"
                + carPlayRole
                + "    </dict>\n"));
        assertFalse(IPhoneBuilder.plistManifestWiresWindowScene(rootBody(shared)),
                "the shared plist has no window role, and must not gain one");

        String mac = IPhoneBuilder.plistForMacSlice(shared);
        assertTrue(IPhoneBuilder.plistManifestSupportsMultipleScenes(rootBody(mac)));
        assertTrue(IPhoneBuilder.plistManifestWiresWindowScene(rootBody(mac)),
                "the Mac copy has to gain the window role, not just the support key");
        assertTrue(mac.contains("CodenameOne_CarPlaySceneDelegate"),
                "and CarPlay's own role survives beside it");
    }

    @Test
    void aManifestWithNoSceneConfigurationsGainsThemOnTheMacSlice() {
        String shared = document(manifest(
                "    <key>UIApplicationSupportsMultipleScenes</key>\n    <false/>\n"));
        String mac = IPhoneBuilder.plistForMacSlice(shared);
        assertTrue(IPhoneBuilder.plistManifestWiresWindowScene(rootBody(mac)),
                "the configurations dictionary is added when there is none");
    }

    @Test
    void aManifestMissingTheSupportKeyGainsItOnTheMacSlice() {
        String shared = document(manifest(
                "    <key>UISceneConfigurations</key>\n    <dict>\n"
                + WINDOW_ROLE
                + "    </dict>\n"));
        String mac = IPhoneBuilder.plistForMacSlice(shared);
        assertTrue(IPhoneBuilder.plistManifestSupportsMultipleScenes(rootBody(mac)),
                "the support key is added when the manifest never declared it");
    }

    @Test
    void aManifestNestedInAnotherDictionaryIsNotTheManifest() {
        // UIKit reads members of the plist root. A manifest parked inside some other
        // dictionary configures nothing, so accepting it would pass a build whose
        // windows are unsupported on the device.
        String fragment = "<key>CN1Metadata</key>\n<dict>\n"
                + manifest("    <key>UIApplicationSupportsMultipleScenes</key>\n    <true/>\n"
                        + "    <key>UISceneConfigurations</key>\n    <dict>\n"
                        + WINDOW_ROLE
                        + "    </dict>\n")
                + "\n</dict>";
        assertFalse(IPhoneBuilder.plistManifestSupportsMultipleScenes(fragment),
                "a manifest nested in another dictionary is not the plist's manifest");
        assertFalse(IPhoneBuilder.plistManifestWiresWindowScene(fragment),
                "and neither is the role inside it");
    }

    @Test
    void aSelfClosingManifestStillGetsItsContentOnTheMacSlice() {
        // "<dict/>" is a valid empty dictionary and an application may well write one.
        // Nothing can add a member to it as written -- there is no closing tag to
        // insert before -- so it has to be expanded first, or the Mac copy comes back
        // unchanged and windows stay unsupported on the device.
        String shared = document("    <key>UIApplicationSceneManifest</key>\n    <dict/>\n");
        String mac = IPhoneBuilder.plistForMacSlice(shared);
        assertTrue(IPhoneBuilder.plistManifestSupportsMultipleScenes(rootBody(mac)),
                "an empty manifest still has to gain multiple-scene support");
        assertTrue(IPhoneBuilder.plistManifestWiresWindowScene(rootBody(mac)),
                "and the window role");
    }

    @Test
    void selfClosingSceneConfigurationsStillGainTheWindowRole() {
        String shared = document(manifest(
                "    <key>UIApplicationSupportsMultipleScenes</key>\n    <false/>\n"
                + "    <key>UISceneConfigurations</key>\n    <dict/>\n"));
        String mac = IPhoneBuilder.plistForMacSlice(shared);
        assertTrue(IPhoneBuilder.plistManifestSupportsMultipleScenes(rootBody(mac)));
        assertTrue(IPhoneBuilder.plistManifestWiresWindowScene(rootBody(mac)),
                "empty scene configurations still have to gain the window role");
    }

    @Test
    void aSelfClosingDictionaryElsewhereIsLeftAlone() {
        // Only the dictionary being added to is expanded; an unrelated one keeps the
        // spelling the application chose.
        String shared = document("    <key>CN1Metadata</key>\n    <dict/>\n");
        String mac = IPhoneBuilder.plistForMacSlice(shared);
        assertTrue(mac.contains("<key>CN1Metadata</key>\n    <dict/>"),
                "an unrelated empty dictionary is not rewritten");
        assertTrue(IPhoneBuilder.plistManifestWiresWindowScene(rootBody(mac)),
                "and the manifest is still added");
    }

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
    void whitespaceOnContainerTagsDoesNotRunTheRoleIntoTheNextOne() {
        // "<array >" and "<dict custom=\"x\">" are the same elements as "<array>" and
        // "<dict>", and plistElementIndex already accepts them. Matching literal tags in
        // the nesting scan found no closing tag at all, so the role fell back to the rest
        // of the fragment -- and a later role's delegate then vouched for a window role
        // that names somebody else. Which is the same hole the CarPlay case above closes,
        // reopened by a space.
        assertFalse(IPhoneBuilder.plistWiresWindowSceneDelegate(
                "<key>UIWindowSceneSessionRoleApplication</key><array ><dict custom=\"x\">"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string>SomeoneElsesSceneDelegate</string></dict></array >"
                        + "<key>CPTemplateApplicationSceneSessionRoleApplication</key><array><dict>"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string>CodenameOne_GLSceneDelegate</string></dict></array>"),
                "a space in the array tags must not extend the window role into CarPlay's");
    }

    @Test
    void whitespaceOnContainerTagsStillAcceptsAValidManifest() {
        // The other direction: the same formatting on a correctly wired manifest has to
        // keep passing, so the rule above cannot be satisfied by rejecting everything.
        assertTrue(IPhoneBuilder.plistWiresWindowSceneDelegate(
                "<key>UIWindowSceneSessionRoleApplication</key><array ><dict custom=\"x\">"
                        + "<key>UISceneNested</key><array ><string>a</string></array >"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string>CodenameOne_GLSceneDelegate</string></dict></array >"),
                "valid XML formatting on container tags must not truncate the role");
    }

    @Test
    void aSelfClosingContainerDoesNotOpenANestingLevel() {
        // "<array/>" is an element but not a level. Counting it as one leaves the depth
        // permanently ahead, the real closing tag is swallowed, and the role runs to the
        // end of the fragment -- which would let a later role's delegate vouch for it.
        assertFalse(IPhoneBuilder.plistWiresWindowSceneDelegate(
                "<key>UIWindowSceneSessionRoleApplication</key><array><dict>"
                        + "<key>UISceneEmptyThing</key><array/>"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string>SomeoneElsesSceneDelegate</string></dict></array>"
                        + "<key>CPTemplateApplicationSceneSessionRoleApplication</key><array><dict>"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string>CodenameOne_GLSceneDelegate</string></dict></array>"),
                "a self-closing array must not extend the window role into the CarPlay role");
    }

    @Test
    void aCloseTagNameThatMerelyStartsTheSameDoesNotClose() {
        // "</arrayish>" starts with "</array" and must not be taken for the array's
        // closing tag; only whitespace may sit between the name and the ">".
        assertTrue(IPhoneBuilder.plistWiresWindowSceneDelegate(
                "<key>UIWindowSceneSessionRoleApplication</key><array><dict>"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string>CodenameOne_GLSceneDelegate</string></dict></array>"));
    }

    @Test
    void aKeyClosedWithWhitespaceIsStillThatKey() {
        // "</key >" closes a key exactly as "</key>" does. Matching the literal reported
        // the key as absent, and the two callers fail in opposite directions from there:
        // the injection path appends a second UIApplicationSceneManifest beside the
        // application's own, and the validation path rejects a build that is correctly
        // configured.
        assertTrue(IPhoneBuilder.plistDeclaresKey(
                "<key>UIApplicationSceneManifest</key >", "UIApplicationSceneManifest"),
                "a key whose closing tag carries whitespace is still declared");
        assertTrue(IPhoneBuilder.plistKeyIsTrue(
                "<key>UIApplicationSupportsMultipleScenes</key ><true/>",
                "UIApplicationSupportsMultipleScenes"),
                "and its value is still readable, which is what plistKeyEnd decides");
    }

    @Test
    void aWholeManifestSurvivesWhitespaceInEveryClosingTag() {
        // The two structural parsers together, over a fragment where every closing tag
        // is spaced. This is valid XML and a build using it must not be rejected.
        assertTrue(IPhoneBuilder.plistWiresWindowSceneDelegate(
                "<key>UIWindowSceneSessionRoleApplication</key ><array ><dict >"
                        + "<key>UISceneDelegateClassName</key >"
                        + "<string>CodenameOne_GLSceneDelegate</string></dict ></array >"),
                "spacing in the closing tags must not stop the delegate being found");
    }

    @Test
    void aStringClosedWithWhitespaceStillHoldsItsValue() {
        // "</string >" ends a string exactly as "</string>" does. Matching the literal
        // made the delegate look absent and aborted a correctly configured build.
        assertTrue(IPhoneBuilder.plistWiresWindowSceneDelegate(
                "<key>UIWindowSceneSessionRoleApplication</key><array><dict>"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string>CodenameOne_GLSceneDelegate</string ></dict></array>"),
                "a delegate whose string tag closes with whitespace is still that "
                        + "delegate");
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

    @Test
    void aCommentedOutWindowRoleDoesNotVouchForTheLiveOne() {
        // A commented-out Codename One configuration sitting above a live role that
        // names another delegate: matching the mention would accept a manifest whose
        // real scene configuration cannot adopt a window, and the failure then happens
        // at run time.
        assertFalse(IPhoneBuilder.plistWiresWindowSceneDelegate(
                "<!-- <key>UIWindowSceneSessionRoleApplication</key><array><dict>"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string>CodenameOne_GLSceneDelegate</string></dict></array> -->"
                        + "<key>UIWindowSceneSessionRoleApplication</key><array><dict>"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string>SomeoneElsesSceneDelegate</string></dict></array>"),
                "the live role names another delegate, so the commented one must not "
                        + "answer for it");
    }

    @Test
    void theClassNameHasToBeTheDelegateNotJustPresent() {
        // Legal manifest: the configuration is *named* after our delegate while the
        // delegate class is somebody else's. Matching the text anywhere in the role
        // accepts it, and that build cannot adopt a secondary window.
        assertFalse(IPhoneBuilder.plistWiresWindowSceneDelegate(
                "<key>UIWindowSceneSessionRoleApplication</key><array><dict>"
                        + "<key>UISceneConfigurationName</key>"
                        + "<string>CodenameOne_GLSceneDelegate</string>"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string>SomeoneElsesSceneDelegate</string></dict></array>"),
                "the class name appears, but not as the delegate");
    }

    @Test
    void oneMatchingConfigurationAmongSeveralIsEnough() {
        // A role may declare more than one configuration; windows can be adopted as
        // long as one of them is ours.
        assertTrue(IPhoneBuilder.plistWiresWindowSceneDelegate(
                "<key>UIWindowSceneSessionRoleApplication</key><array>"
                        + "<dict><key>UISceneDelegateClassName</key>"
                        + "<string>SomeoneElsesSceneDelegate</string></dict>"
                        + "<dict><key>UISceneDelegateClassName</key>"
                        + "<string>CodenameOne_GLSceneDelegate</string></dict></array>"));
    }

    @Test
    void aStringMentioningSceneSessionRoleDoesNotEndTheRole() {
        // Found by re-reading the guard rather than reported. The role's range was
        // bounded by the *text* "SceneSessionRole", so a string value containing those
        // words cut it short and hid a delegate that really is wired -- failing a build
        // that was going to work, which is the expensive direction.
        assertTrue(IPhoneBuilder.plistWiresWindowSceneDelegate(
                "<key>UIWindowSceneSessionRoleApplication</key><array><dict>"
                        + "<key>CFBundleName</key>"
                        + "<string>notes about SceneSessionRole handling</string>"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string>CodenameOne_GLSceneDelegate</string></dict></array>"),
                "a string mentioning the words must not end the role's range");
    }

    @Test
    void aKeyElementMayCarryWhitespaceAroundItsName() {
        // Valid XML. Requiring the tags and the name to be contiguous reported the key
        // absent, and the build then appended a second UIApplicationSceneManifest beside
        // the application's own -- duplicate keys in an ordinary iOS build, not just a
        // Catalyst one.
        assertTrue(IPhoneBuilder.plistDeclaresKey(
                "<key>\n    UIApplicationSceneManifest\n</key><dict/>",
                "UIApplicationSceneManifest"),
                "a key element with whitespace around its name is still that key");
        assertTrue(IPhoneBuilder.plistKeyIsTrue(
                "<key> UIApplicationSupportsMultipleScenes </key><true/>",
                "UIApplicationSupportsMultipleScenes"),
                "and its value is still readable");
        assertTrue(IPhoneBuilder.plistWiresWindowSceneDelegate(
                "<key>\n UIWindowSceneSessionRoleApplication \n</key><array><dict>"
                        + "<key> UISceneDelegateClassName </key>"
                        + "<string>CodenameOne_GLSceneDelegate</string></dict></array>"),
                "and so is the delegate beneath it");
    }

    @Test
    void aDifferentKeyIsStillNotAMatch() {
        // The trim must not turn every key into every other key.
        assertFalse(IPhoneBuilder.plistDeclaresKey(
                "<key>UIApplicationSceneManifestOther</key><dict/>",
                "UIApplicationSceneManifest"));
    }

    @Test
    void aKeyWhoseNameMerelyContainsSceneSessionRoleDoesNotEndTheRole() {
        // A custom key declared inside the role, whose name happens to contain the
        // words. Ending the role there stops the search before the delegate and rejects
        // a manifest that is correctly wired.
        assertTrue(IPhoneBuilder.plistWiresWindowSceneDelegate(
                "<key>UIWindowSceneSessionRoleApplication</key><array><dict>"
                        + "<key>MySceneSessionRoleMetadata</key><string>x</string>"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string>CodenameOne_GLSceneDelegate</string></dict></array>"),
                "a key inside the role must not be taken for the next role");
    }

    @Test
    void theRoleStillEndsAtItsOwnArray() {
        // The boundary still has to hold: a CarPlay role after this one, naming our
        // delegate, must not vouch for a window role that names somebody else.
        assertFalse(IPhoneBuilder.plistWiresWindowSceneDelegate(
                "<key>UIWindowSceneSessionRoleApplication</key><array><dict>"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string>SomeoneElsesSceneDelegate</string></dict></array>"
                        + "<key>CPTemplateApplicationSceneSessionRoleApplication</key>"
                        + "<array><dict><key>UISceneDelegateClassName</key>"
                        + "<string>CodenameOne_GLSceneDelegate</string></dict></array>"),
                "the delegate in the CarPlay role is outside this role's array");
    }

    @Test
    void nestedArraysInsideTheRoleDoNotEndItEarly() {
        assertTrue(IPhoneBuilder.plistWiresWindowSceneDelegate(
                "<key>UIWindowSceneSessionRoleApplication</key><array><dict>"
                        + "<key>SomeList</key><array><string>a</string></array>"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string>CodenameOne_GLSceneDelegate</string></dict></array>"),
                "a nested array must not be mistaken for the role's closing array");
    }

    @Test
    void elementsMayCarryAttributesOrTagWhitespace() {
        // Named as an assumption a round ago and closed here rather than left to be
        // found: these are elements, so "<key >" and "<string xml:space=...>" are the
        // same elements as their bare spellings.
        assertTrue(IPhoneBuilder.plistDeclaresKey(
                "<key >UIApplicationSceneManifest</key><dict/>",
                "UIApplicationSceneManifest"));
        assertTrue(IPhoneBuilder.plistWiresWindowSceneDelegate(
                "<key>UIWindowSceneSessionRoleApplication</key><array><dict>"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string xml:space=\"preserve\">CodenameOne_GLSceneDelegate</string>"
                        + "</dict></array>"),
                "an attribute on the string must not hide the delegate");
    }

    @Test
    void anElementWhoseNameMerelyStartsTheSameIsNotAMatch() {
        // The tolerance must not turn <keyboard> into <key>.
        assertFalse(IPhoneBuilder.plistDeclaresKey(
                "<keyboard>UIApplicationSceneManifest</keyboard>",
                "UIApplicationSceneManifest"));
    }

    @Test
    void anUnrelatedDictionaryDoesNotAnswerForTheManifest() {
        // A custom dictionary that happens to carry the multiple-scenes key as true, in
        // front of a manifest that sets it to false. Asking the whole fragment accepts
        // the build; asking the manifest rejects it, which is the truth of what the
        // bundle will say.
        String plist = "<key>MyCustomConfig</key><dict>"
                + "<key>UIApplicationSupportsMultipleScenes</key><true/>"
                + "<key>UIWindowSceneSessionRoleApplication</key><array><dict>"
                + "<key>UISceneDelegateClassName</key>"
                + "<string>CodenameOne_GLSceneDelegate</string></dict></array></dict>"
                + "<key>UIApplicationSceneManifest</key><dict>"
                + "<key>UIApplicationSupportsMultipleScenes</key><false/></dict>";
        String scope = IPhoneBuilder.plistManifestScope(plist);
        // The whole fragment answers true here, which is the false accept being fixed.
        assertTrue(IPhoneBuilder.plistKeyIsTrue(plist,
                "UIApplicationSupportsMultipleScenes"),
                "unscoped, the unrelated dictionary answers -- this is the bug");
        assertFalse(IPhoneBuilder.plistKeyIsTrue(scope,
                "UIApplicationSupportsMultipleScenes"),
                "the manifest says false, whatever the unrelated dictionary says");
        assertFalse(IPhoneBuilder.plistWiresWindowSceneDelegate(scope),
                "and the role in the unrelated dictionary is not the manifest's");
    }

    @Test
    void theManifestsOwnConfigurationIsStillFound() {
        String plist = "<key>UIApplicationSceneManifest</key><dict>"
                + "<key>UIApplicationSupportsMultipleScenes</key><true/>"
                + "<key>UISceneConfigurations</key><dict>"
                + "<key>UIWindowSceneSessionRoleApplication</key><array><dict>"
                + "<key>UISceneDelegateClassName</key>"
                + "<string>CodenameOne_GLSceneDelegate</string></dict></array></dict></dict>";
        String scope = IPhoneBuilder.plistManifestScope(plist);
        assertTrue(IPhoneBuilder.plistKeyIsTrue(scope,
                "UIApplicationSupportsMultipleScenes"));
        assertTrue(IPhoneBuilder.plistWiresWindowSceneDelegate(scope),
                "a nested UISceneConfigurations dictionary must not put the role out of "
                        + "scope");
    }

    @Test
    void aCommentedClosingTagDoesNotEndTheManifest() {
        // A comment containing "</dict>" ahead of the live configuration. Treating it as
        // the close truncates the range, so validation reads a prefix and rejects a
        // build that is correctly configured.
        String plist = "<key>UIApplicationSceneManifest</key><dict>"
                + "<!-- was </dict> here -->"
                + "<key>UIApplicationSupportsMultipleScenes</key><true/>"
                + "<key>UIWindowSceneSessionRoleApplication</key><array><dict>"
                + "<key>UISceneDelegateClassName</key>"
                + "<string>CodenameOne_GLSceneDelegate</string></dict></array></dict>";
        String scope = IPhoneBuilder.plistManifestScope(plist);
        assertTrue(IPhoneBuilder.plistKeyIsTrue(scope,
                "UIApplicationSupportsMultipleScenes"),
                "the commented closing tag must not end the manifest");
        assertTrue(IPhoneBuilder.plistWiresWindowSceneDelegate(scope),
                "and the role after it is still inside the manifest");
    }
}
