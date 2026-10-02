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

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// The floor arithmetic behind the Xcode 27 deployment-target raise.
///
/// Xcode 27 lifted every Apple platform's minimum at once (iOS and tvOS 12.0 to 15.0, watchOS
/// 4.0 to 9.0, macOS 10.13 to 12.0) and a project under the floor fails outright. The builders
/// respond by raising their target to whatever the SDK reports, so the comparison that decides
/// "is this below the floor" has to be right about macOS-shaped versions in particular.
class AppleSdkFloorTest {

    /// The reason this is not a string comparison. macOS floors are the shape that breaks one:
    /// "10.9" sorts after "10.13" lexically, so a string compare would decide a 10.9 project
    /// already cleared a 10.13 floor and leave it to fail in Xcode instead.
    @Test
    void comparesVersionComponentsNumerically() {
        assertTrue(AppleSdkFloor.compare("10.9", "10.13") < 0, "10.9 is below 10.13");
        assertTrue(AppleSdkFloor.compare("10.13", "10.9") > 0, "10.13 is above 10.9");
        assertTrue(AppleSdkFloor.compare("9.0", "10.0") < 0);
        assertTrue(AppleSdkFloor.compare("14.0", "15.0") < 0, "the iOS raise Xcode 27 made");
        assertTrue(AppleSdkFloor.compare("13.0", "15.0") < 0, "the tvOS raise Xcode 27 made");
        assertTrue(AppleSdkFloor.compare("10.15", "12.0") < 0, "the macOS raise Xcode 27 made");
        assertTrue(AppleSdkFloor.compare("10.0", "9.0") > 0,
                "watchOS is the one slice already above the new floor");
    }

    /// A missing component is zero, so "15" and "15.0" are the same floor. The SDK reports one
    /// spelling and the hints carry the other.
    @Test
    void treatsMissingComponentsAsZero() {
        assertEquals(0, AppleSdkFloor.compare("15", "15.0"));
        assertEquals(0, AppleSdkFloor.compare("15.0.0", "15"));
        assertTrue(AppleSdkFloor.compare("15", "15.1") < 0);
    }

    /// Apple's own strings are not always three integers -- the error text for the iOS 27 floor
    /// names a maximum of "27.0.x". Ordering floors must not throw on one.
    @Test
    void toleratesNonNumericComponents() {
        assertTrue(AppleSdkFloor.compare("27.0", "27.0.x") == 0);
        assertTrue(AppleSdkFloor.compare("26.0", "27.0.x") < 0);
    }

    @Test
    void raisesOnlyUpwards() {
        assertEquals("15.0", AppleSdkFloor.raiseTo("14.0", "15.0"), "below the floor is raised");
        assertEquals("16.0", AppleSdkFloor.raiseTo("16.0", "15.0"), "above the floor is kept");
        assertEquals("15.0", AppleSdkFloor.raiseTo("15.0", "15.0"), "at the floor is kept");
    }

    /// An unknown floor must change nothing. That is the whole non-Mac story: no Xcode to ask,
    /// so the build keeps exactly the target its hints and features chose, as it always did.
    @Test
    void anUnknownFloorChangesNothing() {
        assertEquals("14.0", AppleSdkFloor.raiseTo("14.0", null));
        assertEquals("14.0", AppleSdkFloor.raiseTo("14.0", ""));
        assertEquals("15.0", AppleSdkFloor.raiseTo(null, "15.0"));
    }

    /// The live lookup, where an Xcode exists to ask. Asserts the shape rather than a version,
    /// because the answer is supposed to change when Apple changes it -- pinning the number
    /// here would just be a second place to edit, and would fail on every new Xcode.
    @Test
    void readsARealFloorFromTheInstalledSdk() {
        assumeTrue(new File("/usr/bin/plutil").canExecute(), "needs a Mac");
        assumeTrue(new File("/usr/bin/xcrun").canExecute(), "needs Xcode command line tools");
        String floor = AppleSdkFloor.minimumDeploymentTarget("iphoneos", "/usr/bin/xcrun", null);
        assumeTrue(floor != null, "needs an installed iOS SDK");
        assertTrue(Character.isDigit(floor.charAt(0)), "a version, got: " + floor);
        // Every iOS SDK that can build this tree is well past 8, and no SDK has ever lowered
        // its floor. A floor that suddenly reads as tiny means the plist key moved and the
        // lookup is silently answering with something else.
        assertTrue(AppleSdkFloor.compare(floor, "8.0") > 0,
                "implausibly low floor, the lookup is probably reading the wrong key: " + floor);
    }

    /// A name that is not an SDK must come back null rather than throwing or, worse, returning
    /// a number from some other SDK -- a wrong floor is silently applied to a real build.
    @Test
    void anUnknownSdkHasNoFloor() {
        assumeTrue(new File("/usr/bin/xcrun").canExecute(), "needs Xcode command line tools");
        assertNotNull(AppleSdkFloor.raiseTo("14.0",
                AppleSdkFloor.minimumDeploymentTarget("notasdk", "/usr/bin/xcrun", null)));
        assertEquals("14.0", AppleSdkFloor.raiseTo("14.0",
                AppleSdkFloor.minimumDeploymentTarget("notasdk", "/usr/bin/xcrun", null)));
    }
}
