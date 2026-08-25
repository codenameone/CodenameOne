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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        assertTrue(keyIsTrueAnywhere(
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
        assertTrue(wiresWindowSceneDelegateAnywhere(
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
    void aNestedManifestKeyDoesNotMakeTheFragmentDeclareOne() {
        // The companion to the test below, on the entry guard rather than the scope
        // helpers. A fragment whose only UIApplicationSceneManifest sits inside some
        // unrelated dictionary declares no manifest at all -- plistForMacSlice will add
        // the root member it needs. Searching the whole fragment made the guard say
        // otherwise, the scope lookup then came back empty, and the build was refused
        // for a non-dictionary manifest it never had.
        String fragment = "<key>CN1Metadata</key>\n<dict>\n"
                + manifest("    <key>UIApplicationSupportsMultipleScenes</key>\n    <true/>\n"
                        + "    <key>UISceneConfigurations</key>\n    <dict>\n"
                        + WINDOW_ROLE
                        + "    </dict>\n")
                + "\n</dict>";
        assertNull(IPhoneBuilder.sceneManifestRejection(fragment),
                "a nested manifest key is not a declaration and must not fail the build");
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
    void anInjectionWithNoManifestOfItsOwnIsAccepted() {
        assertNull(IPhoneBuilder.sceneManifestRejection(
                        "<key>CFBundleName</key><string>Demo</string>"),
                "the build writes its own manifest for the Mac slice; there is nothing "
                        + "here to object to");
    }

    @Test
    void aSingleSceneIosManifestIsAccepted() {
        // The point of the Mac-specific copy: an application injecting a manifest for
        // its iOS slice must not be forced to put true in the plist that slice reads.
        assertNull(IPhoneBuilder.sceneManifestRejection(manifest(
                        "    <key>UIApplicationSupportsMultipleScenes</key>\n    <false/>\n")),
                "a single-scene iOS manifest is the right thing to inject");
    }

    @Test
    void aManifestThatIsNotADictionaryIsRejected() {
        // Nothing can be added to it, and left alone the Mac copy is a silent no-op:
        // a build that succeeds and a window unsupported on the device.
        String rejection = IPhoneBuilder.sceneManifestRejection(
                "<key>UIApplicationSceneManifest</key>\n<string>yes please</string>");
        assertNotNull(rejection, "a manifest that is not a dictionary has to be refused");
        assertTrue(rejection.contains("is not a <dict>"), rejection);
    }

    @Test
    void sceneConfigurationsThatAreNotADictionaryAreRejected() {
        String rejection = IPhoneBuilder.sceneManifestRejection(manifest(
                "    <key>UIApplicationSupportsMultipleScenes</key>\n    <false/>\n"
                + "    <key>UISceneConfigurations</key>\n    <array/>\n"));
        assertNotNull(rejection, "configurations that are not a dictionary have to be refused");
        assertTrue(rejection.contains("UISceneConfigurations"), rejection);
    }

    @Test
    void aWindowRoleNamingAnotherDelegateIsRejected() {
        String foreign = WINDOW_ROLE.replace("CodenameOne_GLSceneDelegate", "SomebodyElse");
        String rejection = IPhoneBuilder.sceneManifestRejection(manifest(
                "    <key>UIApplicationSupportsMultipleScenes</key>\n    <false/>\n"
                + "    <key>UISceneConfigurations</key>\n    <dict>\n"
                + foreign
                + "    </dict>\n"));
        assertNotNull(rejection, "we cannot add ours beside theirs; UIKit reads the role");
        assertTrue(rejection.contains("CodenameOne_GLSceneDelegate"), rejection);
    }

    @Test
    void aWindowRoleAlreadyNamingOurDelegateIsAccepted() {
        assertNull(IPhoneBuilder.sceneManifestRejection(manifest(
                        "    <key>UIApplicationSupportsMultipleScenes</key>\n    <false/>\n"
                        + "    <key>UISceneConfigurations</key>\n    <dict>\n"
                        + WINDOW_ROLE
                        + "    </dict>\n")),
                "an application that already wired our delegate has done nothing wrong");
    }

    @Test
    void twoSceneManifestsAreRejectedRatherThanHalfHandled() {
        // A property list resolves a duplicated key to the LAST value, while every
        // lookup here answers with the first -- so validating and rewriting the first
        // would leave the second in force on the device.
        String plist = manifest(
                        "    <key>UIApplicationSupportsMultipleScenes</key>\n    <true/>\n"
                        + "    <key>UISceneConfigurations</key>\n    <dict>\n"
                        + WINDOW_ROLE
                        + "    </dict>\n")
                + "\n"
                + manifest("    <key>UIApplicationSupportsMultipleScenes</key>\n    <false/>\n");
        // The first one is perfectly good, which is what makes this dangerous: every
        // question below answers yes while the bundle ends up with the second.
        assertTrue(IPhoneBuilder.plistManifestSupportsMultipleScenes(plist));
        assertTrue(IPhoneBuilder.plistManifestWiresWindowScene(plist));

        String rejection = IPhoneBuilder.sceneManifestRejection(plist);
        assertNotNull(rejection, "two manifests have to be refused, not half handled");
        assertTrue(rejection.contains("twice"), rejection);
    }

    @Test
    void twoSceneConfigurationsAreRejected() {
        String rejection = IPhoneBuilder.sceneManifestRejection(manifest(
                "    <key>UIApplicationSupportsMultipleScenes</key>\n    <false/>\n"
                + "    <key>UISceneConfigurations</key>\n    <dict>\n"
                + WINDOW_ROLE
                + "    </dict>\n"
                + "    <key>UISceneConfigurations</key>\n    <dict>\n    </dict>\n"));
        assertNotNull(rejection, "two scene configuration dictionaries have to be refused");
        assertTrue(rejection.contains("UISceneConfigurations"), rejection);
    }

    @Test
    void twoWindowRolesAreRejected() {
        String foreign = WINDOW_ROLE.replace("CodenameOne_GLSceneDelegate", "SomebodyElse");
        String rejection = IPhoneBuilder.sceneManifestRejection(manifest(
                "    <key>UIApplicationSupportsMultipleScenes</key>\n    <false/>\n"
                + "    <key>UISceneConfigurations</key>\n    <dict>\n"
                + WINDOW_ROLE
                + foreign
                + "    </dict>\n"));
        assertNotNull(rejection, "two window roles have to be refused");
        assertTrue(rejection.contains("UIWindowSceneSessionRoleApplication"), rejection);
    }

    @Test
    void twoSupportKeysAreRejected() {
        // True first, false last. The rewrite sets the first and the bundle takes the
        // last, so the build succeeds without the support a Window needs.
        String plist = manifest(
                "    <key>UIApplicationSupportsMultipleScenes</key>\n    <true/>\n"
                + "    <key>UIApplicationSupportsMultipleScenes</key>\n    <false/>\n"
                + "    <key>UISceneConfigurations</key>\n    <dict>\n"
                + WINDOW_ROLE
                + "    </dict>\n");
        assertTrue(IPhoneBuilder.plistManifestSupportsMultipleScenes(plist),
                "the first entry answers yes, which is why answering from it was unsafe");
        String rejection = IPhoneBuilder.sceneManifestRejection(plist);
        assertNotNull(rejection, "two support keys have to be refused");
        assertTrue(rejection.contains("UIApplicationSupportsMultipleScenes"), rejection);
    }

    @Test
    void twoSceneDelegatesInOneConfigurationAreRejected() {
        // Ours first, somebody else's last. The wiring check reads the first and the
        // bundle takes the last, so no scene adopts a window.
        String plist = manifest(
                "    <key>UIApplicationSupportsMultipleScenes</key>\n    <true/>\n"
                + "    <key>UISceneConfigurations</key>\n    <dict>\n"
                + "        <key>UIWindowSceneSessionRoleApplication</key>\n"
                + "        <array>\n"
                + "            <dict>\n"
                + "                <key>UISceneDelegateClassName</key>\n"
                + "                <string>CodenameOne_GLSceneDelegate</string>\n"
                + "                <key>UISceneDelegateClassName</key>\n"
                + "                <string>SomebodyElse</string>\n"
                + "            </dict>\n"
                + "        </array>\n"
                + "    </dict>\n");
        assertTrue(IPhoneBuilder.plistManifestWiresWindowScene(plist),
                "the first delegate answers yes, which is why answering from it was unsafe");
        String rejection = IPhoneBuilder.sceneManifestRejection(plist);
        assertNotNull(rejection, "two delegates in one configuration have to be refused");
        assertTrue(rejection.contains("UISceneDelegateClassName"), rejection);
    }

    @Test
    void theMacSliceDropsTheMainNibItWouldOtherwisePairWithAScene() {
        // The default Catalyst build: ios.uiscene is off, so the shared plist keeps
        // NSMainNibFile -- its removal there is gated on that hint. The Mac copy always
        // gains a scene manifest, and a scene lifecycle beside a legacy main NIB is the
        // orphan window FrontBoard terminates at launch. It is also excluded from the
        // Mac slice's compilation, so the key names a NIB that is not in that bundle.
        String shared = document(
                "    <key>NSMainNibFile</key>\n    <string>MainWindow</string>\n");
        assertTrue(shared.contains("NSMainNibFile"),
                "the shared plist keeps it, which is what the iOS slice needs");

        String mac = IPhoneBuilder.plistForMacSlice(shared);
        assertFalse(mac.contains("NSMainNibFile"),
                "the Mac copy must not pair a scene manifest with a main NIB");
        assertTrue(IPhoneBuilder.plistManifestWiresWindowScene(rootBody(mac)),
                "and it still gains the manifest, which is why the NIB had to go");
        assertTrue(mac.contains("<key>CFBundleName</key>"),
                "nothing else is disturbed");
    }

    @Test
    void aProcessingInstructionBetweenAKeyAndItsValueIsSkippedWhole() {
        // The parser ignores the whole instruction, so the manifest's value is still the
        // <dict> after it. Advancing only past "<?" landed inside the instruction's data
        // and returned the element name written there, so the manifest was reported as
        // something other than a dictionary and a valid Catalyst build was refused.
        String inject = "<key>UIApplicationSceneManifest</key>\n"
                + "    <?cn1 <string>note</string> ?>\n"
                + "    <dict>\n"
                + "        <key>UIApplicationSupportsMultipleScenes</key>\n        <true/>\n"
                + "        <key>UISceneConfigurations</key>\n        <dict>\n"
                + WINDOW_ROLE
                + "        </dict>\n"
                + "    </dict>\n";
        assertNull(IPhoneBuilder.sceneManifestRejection(inject),
                "the instruction is ignored and the <dict> after it is the value");
    }

    @Test
    void aCdataMarkerInsideACommentIsNotACdataSection() {
        // The parser discards the comment whole, so this key reads as NSMainNibFile.
        // Discovering CDATA with a raw search ran before comments were dropped, so the
        // marker written inside the comment opened a CDATA section that never closes --
        // the key went unrecognized and its entry stayed in the Catalyst plist.
        String shared = document(
                "    <key>NSMain<!-- <![CDATA[ example -->NibFile</key>\n"
                + "    <string>MainWindow</string>\n");
        assertFalse(IPhoneBuilder.plistForMacSlice(shared).contains("MainWindow"),
                "a CDATA marker inside a comment is comment text, not a section");
    }

    @Test
    void aCommentMarkerInsideCdataIsStillLiteralText() {
        // The converse, which must keep working: inside a real CDATA section "<!--" is
        // ordinary text, so it must not be treated as a comment and dropped.
        assertEquals("a<!--b", WatchNativeBuilder.plistStringContent("a<![CDATA[<!--]]>b"),
                "CDATA content is literal, including markup that looks like a comment");
    }

    @Test
    void aKeyInterruptedByAProcessingInstructionIsStillThatKey() {
        // A PI inside element content is markup the parser drops, exactly as a comment
        // is, so this key resolves to NSMainNibFile. Leaving the PI in the resolved text
        // meant the key was not recognized, the entry stayed in the Catalyst plist, and
        // it paired with the scene manifest added below.
        String shared = document(
                "    <key>NSMain<?cn1 note?>NibFile</key>\n    <string>MainWindow</string>\n");
        String mac = IPhoneBuilder.plistForMacSlice(shared);
        assertFalse(mac.contains("MainWindow"),
                "a PI inside the key does not change which key it is");
    }

    @Test
    void markupInsideAProcessingInstructionIsNotADeclaration() {
        // A processing instruction's data is not element content -- a plist parser
        // ignores the whole PI. Scanning that looked only past comments and CDATA, so
        // a PI whose data resembles a manifest was read as one: the validation approved
        // it, and plistForMacSlice rewrote the PI instead of adding a real root
        // manifest, producing a build with no scene configuration for Window at all.
        String inject = "<key>Unrelated</key>\n    <string>x</string>\n"
                + "    <?cn1 <key>UIApplicationSceneManifest</key><dict/> ?>\n";
        assertNull(IPhoneBuilder.sceneManifestRejection(inject),
                "a processing instruction declares nothing");
        assertFalse(IPhoneBuilder.plistDeclaresKey(inject, "UIApplicationSceneManifest"),
                "and the key inside it is not a declaration either");
    }

    @Test
    void aDelegateNameSpelledWithCharacterDataStillNamesThatDelegate() {
        // The value side of the same question. A plist parser reads both of these as
        // CodenameOne_GLSceneDelegate, so the manifest is correctly wired; comparing the
        // raw serialization reported some other delegate and refused the build.
        String cdata = manifest(
                "    <key>UIApplicationSupportsMultipleScenes</key>\n    <true/>\n"
                + "    <key>UISceneConfigurations</key>\n    <dict>\n"
                + "        <key>UIWindowSceneSessionRoleApplication</key>\n"
                + "        <array>\n"
                + "            <dict>\n"
                + "                <key>UISceneDelegateClassName</key>\n"
                + "                <string><![CDATA[CodenameOne_GLSceneDelegate]]></string>\n"
                + "            </dict>\n"
                + "        </array>\n"
                + "    </dict>\n");
        assertTrue(IPhoneBuilder.plistManifestWiresWindowScene(rootBody(document(cdata))),
                "a CDATA-spelled delegate name still names that delegate");

        String entity = cdata.replace("<![CDATA[CodenameOne_GLSceneDelegate]]>",
                "CodenameOne_GLSceneDelegat&#x65;");
        assertTrue(IPhoneBuilder.plistManifestWiresWindowScene(rootBody(document(entity))),
                "and so does one whose last letter is a character reference");
    }

    @Test
    void aKeySpelledWithCdataIsStillThatKey() {
        // A plist parser resolves <key><![CDATA[NSMainNibFile]]></key> to NSMainNibFile,
        // so this IS a main NIB declaration. Matching the raw serialization missed it,
        // and the Mac copy then kept the effective entry and paired it with the scene
        // manifest added below -- the launch failure the removal exists to prevent.
        String shared = document(
                "    <key><![CDATA[NSMainNibFile]]></key>\n    <string>MainWindow</string>\n");
        String mac = IPhoneBuilder.plistForMacSlice(shared);
        assertFalse(mac.contains("NSMainNibFile"),
                "a CDATA-spelled key names the same key and has to go with it");
        assertFalse(mac.contains("MainWindow"), "and its value goes with it");
        assertTrue(IPhoneBuilder.plistManifestWiresWindowScene(rootBody(mac)),
                "the Mac copy still gains the manifest that made the NIB fatal");
    }

    @Test
    void aKeyCarryingACommentOrAnEntityIsStillThatKey() {
        // Same question, the other two legal spellings: a comment inside the element,
        // and a character reference. Both resolve to the same name.
        String commented = document(
                "    <key>NSMain<!-- why -->NibFile</key>\n    <string>MainWindow</string>\n");
        assertFalse(IPhoneBuilder.plistForMacSlice(commented).contains("MainWindow"),
                "a comment inside the key does not change which key it is");

        String entity = document(
                "    <key>&#78;SMainNibFile</key>\n    <string>MainWindow</string>\n");
        assertFalse(IPhoneBuilder.plistForMacSlice(entity).contains("MainWindow"),
                "nor does spelling its first letter as a character reference");
    }

    @Test
    void markupInsideACdataSectionIsNotADeclaration() {
        // Character data that happens to look like markup. The element search excluded
        // only XML comments, so this was read as a live declaration: plistDeclaresKey
        // said a manifest was present, plistManifestScope then found nothing at the
        // fragment root, and an entirely valid Catalyst build was refused for having a
        // manifest that "is not a <dict>".
        String inject = "<key>SomeUnrelatedString</key>\n"
                + "<string><![CDATA[<key>UIApplicationSceneManifest</key>]]></string>\n";
        assertNull(IPhoneBuilder.sceneManifestRejection(inject),
                "CDATA content is character data, not a manifest declaration");
    }

    @Test
    void aCommentOpenerInsideCdataDoesNotHideTheRestOfTheFragment() {
        // The old check searched BACKWARDS for "<!--". A "<!--" written inside CDATA is
        // literal text, but it looked like an unterminated comment, so everything after
        // it became invisible -- including a genuinely malformed manifest, which was
        // then accepted and shipped as a Window-less build.
        String inject = "<key>SomeUnrelatedString</key>\n"
                + "<string><![CDATA[<!-- not really a comment]]></string>\n"
                + "<key>UIApplicationSceneManifest</key>\n"
                + "<string>not a dictionary</string>\n";
        String rejection = IPhoneBuilder.sceneManifestRejection(inject);
        assertNotNull(rejection,
                "the manifest after the CDATA section is still there and is still wrong");
        assertTrue(rejection.contains("<dict>"), rejection);
    }

    @Test
    void theMacSliceDropsAnInjectedMainNibThatShadowsTheTemplateOne() {
        // ios.plistInject appends its members after the template's, so a project that
        // injects its own NSMainNibFile produces a root dictionary carrying the key
        // twice. CFPropertyList resolves a duplicate to the LAST entry, so the injected
        // one is the effective one -- while every lookup in this file finds the first.
        // Dropping a single match would therefore delete the entry iOS was already
        // ignoring and leave the live one to pair with the manifest added below.
        String shared = document(
                "    <key>NSMainNibFile</key>\n    <string>MainWindow</string>\n"
                + "    <key>NSMainNibFile</key>\n    <string>InjectedWindow</string>\n");

        String mac = IPhoneBuilder.plistForMacSlice(shared);
        assertFalse(mac.contains("NSMainNibFile"),
                "both entries have to go, not just the one that was being ignored");
        assertFalse(mac.contains("InjectedWindow"),
                "the injected entry is the effective one and is what actually breaks launch");
        assertTrue(IPhoneBuilder.plistManifestWiresWindowScene(rootBody(mac)),
                "and the Mac copy still gains the manifest that made the NIB fatal");
        assertTrue(mac.contains("<key>CFBundleName</key>"),
                "nothing else is disturbed");
    }

    @Test
    void aPlistWithNoMainNibIsUnharmed() {
        String mac = IPhoneBuilder.plistForMacSlice(document(
                "    <key>UILaunchStoryboardName</key>\n    <string>LaunchScreen</string>\n"));
        assertTrue(mac.contains("<key>UILaunchStoryboardName</key>"),
                "a plist that never named a main NIB keeps everything it had");
        assertTrue(IPhoneBuilder.plistManifestWiresWindowScene(rootBody(mac)));
    }

    @Test
    void oneOfEachReservedKeyIsStillAccepted() {
        // The duplicate checks must not fire on a well formed manifest, which is the
        // way a rejection rule usually goes wrong.
        assertNull(IPhoneBuilder.sceneManifestRejection(manifest(
                        "    <key>UIApplicationSupportsMultipleScenes</key>\n    <false/>\n"
                        + "    <key>UISceneConfigurations</key>\n    <dict>\n"
                        + WINDOW_ROLE
                        + "    </dict>\n")));
    }

    @Test
    void aKeySetToFalseIsNotAcceptedAsTrue() {
        assertFalse(keyIsTrueAnywhere(
                "<key>UIApplicationSupportsMultipleScenes</key><false/>",
                "UIApplicationSupportsMultipleScenes"),
                "the key is present but says false, which is the case that has to be caught");
    }

    @Test
    void aKeySetToTrueIsAccepted() {
        assertTrue(keyIsTrueAnywhere(
                "<key>UIApplicationSupportsMultipleScenes</key>\n    <true/>",
                "UIApplicationSupportsMultipleScenes"));
    }

    @Test
    void aLaterUnrelatedTrueDoesNotVouchForThisKey() {
        // The value of a key is the element that follows it. A <true/> belonging to
        // some other key further down says nothing about this one.
        assertFalse(keyIsTrueAnywhere(
                "<key>UIApplicationSupportsMultipleScenes</key><false/>\n"
                        + "<key>UISomethingElse</key><true/>",
                "UIApplicationSupportsMultipleScenes"),
                "a true further down the plist belongs to a different key");
        assertFalse(keyIsTrueAnywhere(
                "<key>UIApplicationSupportsMultipleScenes</key>\n"
                        + "<key>UISomethingElse</key><true/>",
                "UIApplicationSupportsMultipleScenes"),
                "another key intervenes, so this one has no true of its own");
    }

    @Test
    void anAbsentKeyIsNotTrue() {
        assertFalse(keyIsTrueAnywhere("<key>UIOther</key><true/>",
                "UIApplicationSupportsMultipleScenes"));
    }

    @Test
    void theWindowRoleHasToNameCodenameOnesDelegate() {
        assertTrue(wiresWindowSceneDelegateAnywhere(
                "<key>UIWindowSceneSessionRoleApplication</key><array><dict>"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string>CodenameOne_GLSceneDelegate</string></dict></array>"));
        assertFalse(wiresWindowSceneDelegateAnywhere(
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
        assertFalse(wiresWindowSceneDelegateAnywhere(
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
        assertTrue(wiresWindowSceneDelegateAnywhere(
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
        assertFalse(wiresWindowSceneDelegateAnywhere(
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
        assertTrue(wiresWindowSceneDelegateAnywhere(
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
        assertTrue(keyIsTrueAnywhere(
                "<key>UIApplicationSupportsMultipleScenes</key ><true/>",
                "UIApplicationSupportsMultipleScenes"),
                "and its value is still readable, which is what plistKeyEnd decides");
    }

    @Test
    void aWholeManifestSurvivesWhitespaceInEveryClosingTag() {
        // The two structural parsers together, over a fragment where every closing tag
        // is spaced. This is valid XML and a build using it must not be rejected.
        assertTrue(wiresWindowSceneDelegateAnywhere(
                "<key>UIWindowSceneSessionRoleApplication</key ><array ><dict >"
                        + "<key>UISceneDelegateClassName</key >"
                        + "<string>CodenameOne_GLSceneDelegate</string></dict ></array >"),
                "spacing in the closing tags must not stop the delegate being found");
    }

    @Test
    void aStringClosedWithWhitespaceStillHoldsItsValue() {
        // "</string >" ends a string exactly as "</string>" does. Matching the literal
        // made the delegate look absent and aborted a correctly configured build.
        assertTrue(wiresWindowSceneDelegateAnywhere(
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
        assertFalse(wiresWindowSceneDelegateAnywhere(
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
            assertTrue(keyIsTrueAnywhere(
                    "<key>UIApplicationSupportsMultipleScenes</key>" + spelling,
                    "UIApplicationSupportsMultipleScenes"),
                    "should accept " + spelling);
        }
    }

    @Test
    void theValidXmlSpellingsOfFalseAreAllRejected() {
        for (String spelling : new String[]{"<false/>", "<false />", "<false></false>"}) {
            assertFalse(keyIsTrueAnywhere(
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
        assertFalse(keyIsTrueAnywhere(
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
        assertFalse(keyIsTrueAnywhere(
                "<key>UIApplicationSupportsMultipleScenes</key>"
                        + "<!-- <true/> was here --><false/>",
                "UIApplicationSupportsMultipleScenes"),
                "the commented-out true must not stand in for the real false");
        assertTrue(keyIsTrueAnywhere(
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
        assertFalse(wiresWindowSceneDelegateAnywhere(
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
        assertFalse(wiresWindowSceneDelegateAnywhere(
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
        assertTrue(wiresWindowSceneDelegateAnywhere(
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
        assertTrue(wiresWindowSceneDelegateAnywhere(
                "<key>UIWindowSceneSessionRoleApplication</key><array><dict>"
                        + "<key>CFBundleName</key>"
                        + "<string>notes about SceneSessionRole handling</string>"
                        + "<key>UISceneDelegateClassName</key>"
                        + "<string>CodenameOne_GLSceneDelegate</string></dict></array>"),
                "a string mentioning the words must not end the role's range");
    }

    @Test
    void aKeyElementsWhitespaceIsPartOfItsName() {
        // Verified against Foundation itself rather than assumed. plutil on
        //   <key> PaddedKey </key><string>padded</string>
        //   <key>\n\tMultilineKey\n\t</key><string>multiline</string>
        // yields {" PaddedKey ":"padded", "\n\tMultilineKey\n\t":"multiline"} -- the
        // padding is part of the key. So a padded UIApplicationSceneManifest is a
        // DIFFERENT key that UIKit never reads, and the app has no manifest.
        //
        // This test previously asserted the opposite, on the belief that such a key was
        // "still that key". Trimming to match it meant validation and plistForMacSlice
        // took a padded custom key for the real manifest and rewrote that instead of
        // adding a root one, so the Catalyst build succeeded with no effective manifest
        // and Window unsupported.
        assertFalse(IPhoneBuilder.plistDeclaresKey(
                "<key>\n    UIApplicationSceneManifest\n</key><dict/>",
                "UIApplicationSceneManifest"),
                "padding is part of the name, so this is not that key");
        assertFalse(keyIsTrueAnywhere(
                "<key> UIApplicationSupportsMultipleScenes </key><true/>",
                "UIApplicationSupportsMultipleScenes"),
                "nor is a padded support key");
        assertFalse(wiresWindowSceneDelegateAnywhere(
                "<key>\n UIWindowSceneSessionRoleApplication \n</key><array><dict>"
                        + "<key> UISceneDelegateClassName </key>"
                        + "<string>CodenameOne_GLSceneDelegate</string></dict></array>"),
                "nor a padded role or delegate key");
    }

    @Test
    void aPaddedManifestKeyGetsARealOneAddedBesideIt() {
        // The consequence that matters. The application's padded key is inert, so the
        // Mac slice must not rewrite it and call the job done -- it has to add a real,
        // exactly spelled root manifest. There is no duplicate-key hazard in doing so,
        // because Foundation reads the two as different keys.
        String shared = document(
                "    <key> UIApplicationSceneManifest </key>\n    <string>inert</string>\n");
        String mac = IPhoneBuilder.plistForMacSlice(shared);
        assertTrue(IPhoneBuilder.plistManifestWiresWindowScene(rootBody(mac)),
                "the Mac copy has to gain a manifest UIKit will actually read");
        assertTrue(mac.contains("<key> UIApplicationSceneManifest </key>"),
                "and the application's own key is left exactly as written");
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
        assertTrue(wiresWindowSceneDelegateAnywhere(
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
        assertFalse(wiresWindowSceneDelegateAnywhere(
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
        assertTrue(wiresWindowSceneDelegateAnywhere(
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
        assertTrue(wiresWindowSceneDelegateAnywhere(
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
        assertTrue(keyIsTrueAnywhere(plist,
                "UIApplicationSupportsMultipleScenes"),
                "unscoped, the unrelated dictionary answers -- this is the bug");
        assertFalse(keyIsTrueAnywhere(scope,
                "UIApplicationSupportsMultipleScenes"),
                "the manifest says false, whatever the unrelated dictionary says");
        assertFalse(wiresWindowSceneDelegateAnywhere(scope),
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
        assertTrue(keyIsTrueAnywhere(scope,
                "UIApplicationSupportsMultipleScenes"));
        assertTrue(wiresWindowSceneDelegateAnywhere(scope),
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
        assertTrue(keyIsTrueAnywhere(scope,
                "UIApplicationSupportsMultipleScenes"),
                "the commented closing tag must not end the manifest");
        assertTrue(wiresWindowSceneDelegateAnywhere(scope),
                "and the role after it is still inside the manifest");
    }
    /**
     * The unscoped questions this validation used to ask, kept here rather than in the
     * builder because nothing there asks them any more: every real question is scoped
     * to the dictionary UIKit reads it from. They stay because the cases below are
     * really about the parser underneath -- whitespace in closing tags, comments,
     * self-closing containers, nesting, attributes -- and asking it through the
     * simplest possible wrapper is the clearest way to reach it.
     *
     * <p>Leaving them in the builder would have been worse than dead weight: an
     * "is this key true anywhere in here" helper sitting beside the scoped ones is an
     * invitation to reach for it again, and reaching for it is what produced three
     * rounds of nesting defects.</p>
     */
    private static boolean keyIsTrueAnywhere(String plist, String key) {
        int at = IPhoneBuilder.plistKeyIndex(plist, key);
        if (at < 0) {
            return false;
        }
        // The value of a key is the element that follows it, so read that element's
        // name rather than matching a spelling: "<true/>", "<true />" and
        // "<true></true>" are the same element.
        return "true".equals(
                IPhoneBuilder.nextElementName(plist, IPhoneBuilder.plistKeyEnd(plist, at)));
    }

    /** The unscoped window-role question; see {@link #keyIsTrueAnywhere}. */
    private static boolean wiresWindowSceneDelegateAnywhere(String plist) {
        int role = IPhoneBuilder.plistKeyIndex(plist, "UIWindowSceneSessionRoleApplication");
        if (role < 0) {
            return false;
        }
        int afterKey = IPhoneBuilder.plistKeyEnd(plist, role);
        // Bounded by this role's own value element, so a CarPlay configuration cannot
        // answer for it and nothing declared inside the role can end it early.
        int end = IPhoneBuilder.plistValueElementEnd(plist, afterKey);
        if (end < 0) {
            end = plist.length();
        }
        // Bound to its key rather than found anywhere in the role: the class name can
        // appear as some other live value while UISceneDelegateClassName names
        // somebody else.
        int at = IPhoneBuilder.plistKeyIndex(plist, "UISceneDelegateClassName", afterKey);
        while (at >= 0 && at < end) {
            if ("CodenameOne_GLSceneDelegate".equals(IPhoneBuilder.plistStringValueAfter(
                    plist, IPhoneBuilder.plistKeyEnd(plist, at)))) {
                return true;
            }
            at = IPhoneBuilder.plistKeyIndex(plist, "UISceneDelegateClassName",
                    IPhoneBuilder.plistKeyEnd(plist, at));
        }
        return false;
    }
}
