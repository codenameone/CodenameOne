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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Comparing an explicit `ios.deployment_target` against the SDK's floor.
///
/// The floor check runs on every Mac build, so it meets whatever a project happens to have
/// written in codenameone_settings.properties -- including the spellings that have always
/// been harmless because they never reached a parser.
class IPhoneBuilderDeploymentFloorPinTest {

    private static int compareVersionStrings(String a, String b) throws Exception {
        Method m = IPhoneBuilder.class.getDeclaredMethod(
                "compareVersionStrings", String.class, String.class);
        m.setAccessible(true);
        return (Integer) m.invoke(null, a, b);
    }

    /// Why the floor check does not use compareVersionStrings. Every other caller reaches it
    /// through maxVersionString, which trims entries and drops empty ones first, so these
    /// inputs never got that far and were silently tolerated. Passing them straight in throws.
    /// If this test ever stops throwing, compareVersionStrings grew its own tolerance and the
    /// guard in pinnedTargetIsBelow can be reconsidered.
    @Test
    void compareVersionStringsStillCannotTakeABlankOrPaddedVersion() {
        InvocationTargetException empty = assertThrows(InvocationTargetException.class,
                () -> compareVersionStrings("", "15.0"));
        assertInstanceOf(NumberFormatException.class, empty.getCause(),
                "an empty component reaches Integer.parseInt");

        InvocationTargetException padded = assertThrows(InvocationTargetException.class,
                () -> compareVersionStrings(" 14.0 ", "15.0"));
        assertInstanceOf(NumberFormatException.class, padded.getCause(),
                "Integer.parseInt does not trim");
    }

    /// A hint written as `codename1.arg.ios.deployment_target=` arrives as a non-null empty
    /// string, not null. Nothing was asked for, so there is nothing below the floor and
    /// nothing to report -- and above all, no exception on a build that used to work.
    @Test
    void aBlankPinIsNotAPin() {
        assertFalse(IPhoneBuilder.pinnedTargetIsBelow(null, "15.0"));
        assertFalse(IPhoneBuilder.pinnedTargetIsBelow("", "15.0"));
        assertFalse(IPhoneBuilder.pinnedTargetIsBelow("   ", "15.0"));
    }

    /// Surrounding whitespace is a typo, not a different version.
    @Test
    void aPaddedPinIsCompared() {
        assertTrue(IPhoneBuilder.pinnedTargetIsBelow(" 14.0 ", "15.0"),
                "a padded 14.0 is still below a 15.0 floor");
        assertFalse(IPhoneBuilder.pinnedTargetIsBelow(" 16.0 ", "15.0"));
    }

    /// The cases that actually matter, with the numbers Xcode 27 introduced.
    @Test
    void reportsOnlyAPinBelowTheFloor() {
        assertTrue(IPhoneBuilder.pinnedTargetIsBelow("13.0", "15.0"));
        assertTrue(IPhoneBuilder.pinnedTargetIsBelow("14.0", "15.0"));
        assertFalse(IPhoneBuilder.pinnedTargetIsBelow("15.0", "15.0"), "at the floor is fine");
        assertFalse(IPhoneBuilder.pinnedTargetIsBelow("16.0", "15.0"));
        assertFalse(IPhoneBuilder.pinnedTargetIsBelow("15", "15.0"),
                "a missing component is zero, so 15 and 15.0 are the same floor");
    }
}
