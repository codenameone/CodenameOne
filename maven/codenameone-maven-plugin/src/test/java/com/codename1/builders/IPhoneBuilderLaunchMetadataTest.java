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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Apple rejects an app linked with the iOS 27 SDK that declares no launch screen, and an
 * app linked with it that does not adopt the scene lifecycle does not launch at all. Both
 * of those are decided by the finished Info.plist, and both used to be reachable from a
 * build hint that succeeded. These are the two refusals that close them, plus the version
 * parse both are conditional on -- because getting that wrong either applies the rules to
 * an SDK Apple exempts or applies them to none.
 */
class IPhoneBuilderLaunchMetadataTest {

    private static final String LAUNCH_SCREEN =
            "    <key>UILaunchScreen</key>\n"
            + "    <dict>\n"
            + "        <key>UIImageName</key>\n"
            + "        <string>Launch.Foreground</string>\n"
            + "    </dict>\n";

    private static final String SCENE_MANIFEST =
            "    <key>UIApplicationSceneManifest</key>\n"
            + "    <dict>\n"
            + "        <key>UIApplicationSupportsMultipleScenes</key>\n"
            + "        <false/>\n"
            + "        <key>UISceneConfigurations</key>\n"
            + "        <dict>\n"
            + IPhoneBuilder.WINDOW_SCENE_ROLE
            + "        </dict>\n"
            + "    </dict>\n";

    /// A whole Info.plist document, which is what the validator is handed: it reads the
    /// root dictionary, and a bare fragment has none.
    private static String document(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<plist version=\"1.0\">\n<dict>\n"
                + "    <key>CFBundleName</key>\n    <string>Demo</string>\n"
                + body
                + "</dict>\n</plist>\n";
    }

    @Test
    void sdkMajorIsTheMajorAloneAndNeverTheDigitsGathered() {
        assertEquals(27, IPhoneBuilder.iosSdkMajorVersion("iphoneos27.2"));
        assertEquals(27, IPhoneBuilder.iosSdkMajorVersion("iphoneos27.0"));
        assertEquals(27, IPhoneBuilder.iosSdkMajorVersion("iphoneos27"));
        assertEquals(26, IPhoneBuilder.iosSdkMajorVersion("iphoneos26.0"));
        assertEquals(18, IPhoneBuilder.iosSdkMajorVersion("iphonesimulator18.4"));
    }

    @Test
    void anUnversionedSdkNameIsUnknownRatherThanAnyParticularVersion() {
        // activeIosSdkName answers the bare platform off a machine that cannot be asked, and
        // there it deliberately means "every version". Reading it as a number here would have
        // to pick one, and either choice is wrong: -1 hands the decision back to the caller.
        assertEquals(-1, IPhoneBuilder.iosSdkMajorVersion("iphoneos"));
        assertEquals(-1, IPhoneBuilder.iosSdkMajorVersion(""));
        assertEquals(-1, IPhoneBuilder.iosSdkMajorVersion(null));
    }

    @Test
    void sceneLifecycleOptOutIsRefusedAgainstSdk27() {
        String rejection = IPhoneBuilder.sceneLifecycleOptOutRejection("false", 27);
        assertNotNull(rejection);
        assertTrue(rejection.contains("ios.uiscene"), rejection);
        assertTrue(rejection.contains("TN3187"), rejection);
        assertNotNull(IPhoneBuilder.sceneLifecycleOptOutRejection("false", 28));
        // Case and surrounding space come straight from a properties file, so neither can be
        // the difference between refusing the build and shipping one that will not launch.
        assertNotNull(IPhoneBuilder.sceneLifecycleOptOutRejection("FALSE", 27));
        assertNotNull(IPhoneBuilder.sceneLifecycleOptOutRejection("  false  ", 27));
    }

    @Test
    void sceneLifecycleOptOutStillWorksOnTheSdksApplePermitsItOn() {
        assertNull(IPhoneBuilder.sceneLifecycleOptOutRejection("false", 26));
        assertNull(IPhoneBuilder.sceneLifecycleOptOutRejection("false", 16));
        // Unknown SDK: nothing is known to require the lifecycle, so nothing is refused. The
        // build() fallback resolves -1 before this is reached; this is the belt to that brace.
        assertNull(IPhoneBuilder.sceneLifecycleOptOutRejection("false", -1));
    }

    @Test
    void theDefaultAndAnExplicitTrueAreNeverRefused() {
        assertNull(IPhoneBuilder.sceneLifecycleOptOutRejection("true", 27));
        assertNull(IPhoneBuilder.sceneLifecycleOptOutRejection(null, 27));
    }

    @Test
    void aBundleWithLaunchScreenAndSceneManifestPasses() {
        assertNull(IPhoneBuilder.launchMetadataRejection(
                document(LAUNCH_SCREEN + SCENE_MANIFEST), 27));
    }

    @Test
    void allFourOfApplesLaunchKeysAreAccepted() {
        // The check is that a launch experience exists, never a preference for the one this
        // builder generates: an app is entitled to declare whichever of the four describes it.
        String[] declarations = {
            "    <key>UILaunchStoryboardName</key>\n    <string>LaunchScreen</string>\n",
            "    <key>UILaunchStoryboards</key>\n    <dict/>\n",
            LAUNCH_SCREEN,
            "    <key>UILaunchScreens</key>\n    <dict/>\n",
        };
        for (String declaration : declarations) {
            assertNull(IPhoneBuilder.launchMetadataRejection(
                    document(declaration + SCENE_MANIFEST), 27), declaration);
        }
    }

    @Test
    void aBundleWithNoLaunchKeyIsRefused() {
        String rejection = IPhoneBuilder.launchMetadataRejection(
                document("    <key>UIRequiresFullScreen</key>\n    <true/>\n" + SCENE_MANIFEST), 27);
        assertNotNull(rejection);
        assertTrue(rejection.contains("launch screen"), rejection);
        // The exact confusion this exists to catch: UIRequiresFullScreen is present, and it is
        // not one of the four.
        assertTrue(rejection.contains("UIRequiresFullScreen"), rejection);
    }

    @Test
    void aBundleWithNoSceneManifestIsRefused() {
        String rejection = IPhoneBuilder.launchMetadataRejection(document(LAUNCH_SCREEN), 27);
        assertNotNull(rejection);
        assertTrue(rejection.contains("UIApplicationSceneManifest"), rejection);
    }

    @Test
    void aNestedNamesakeIsNotADeclaration() {
        // UIKit reads these off the root of the bundle's Info.plist. A key of the same name
        // buried in some other dictionary is invisible to it, and accepting one would let a
        // plistInject satisfy the check with something the device never sees.
        String buried =
                "    <key>SomeVendorConfiguration</key>\n"
                + "    <dict>\n"
                + LAUNCH_SCREEN
                + "    </dict>\n";
        assertNotNull(IPhoneBuilder.launchMetadataRejection(document(buried + SCENE_MANIFEST), 27));
    }

    @Test
    void aKeyNamedOnlyInACommentIsNotADeclaration() {
        String commented = "    <!-- <key>UILaunchStoryboardName</key> -->\n";
        assertNotNull(IPhoneBuilder.launchMetadataRejection(
                document(commented + SCENE_MANIFEST), 27));
    }

    @Test
    void nothingIsRefusedOnTheSdksApplesRuleDoesNotReach() {
        String bare = document("    <key>UIRequiresFullScreen</key>\n    <true/>\n");
        assertNull(IPhoneBuilder.launchMetadataRejection(bare, 26));
        assertNull(IPhoneBuilder.launchMetadataRejection(bare, -1));
    }

    @Test
    void anUnreadableDocumentIsNotDiagnosedAsALaunchScreenFailure() {
        // Xcode has its own opinion about a malformed plist and states it clearly. Answering
        // "no launch screen" for one would send the developer after the wrong problem.
        assertNull(IPhoneBuilder.launchMetadataRejection("not a plist at all", 27));
        assertNull(IPhoneBuilder.launchMetadataRejection("", 27));
    }
}
