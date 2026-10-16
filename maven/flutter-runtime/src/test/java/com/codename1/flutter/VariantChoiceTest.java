/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Flutter's AssetImage._findBestVariant, which depends on the variants present.
class VariantChoiceTest {

    @Test
    void theNearestLowerVariantWinsBelowTheMidpoint() {
        assertEquals(2.0, FlutterAssets.chooseVariant(2.1, new double[] {1.0, 2.0, 3.0}), 0.0,
                "2.1 is nearer 2.0 than 3.0");
        assertEquals(3.0, FlutterAssets.chooseVariant(2.6, new double[] {1.0, 2.0, 3.0}), 0.0);
    }

    @Test
    void whatIsMissingChangesTheChoice() {
        // Without 2.0x the neighbours are 1.5x and 3.0x, midpoint 2.25.
        assertEquals(1.5, FlutterAssets.chooseVariant(2.1, new double[] {1.0, 1.5, 3.0}), 0.0);
    }

    @Test
    void lowDensityScreensPreferTheSharperVariant() {
        assertEquals(2.0, FlutterAssets.chooseVariant(1.1, new double[] {1.0, 2.0}), 0.0,
                "below 2.0 the upper variant wins even when the lower is nearer");
    }

    @Test
    void anExactMatchOrAOneSidedChoice() {
        assertEquals(3.0, FlutterAssets.chooseVariant(3.0, new double[] {1.0, 3.0}), 0.0);
        assertEquals(1.0, FlutterAssets.chooseVariant(3.0, new double[] {1.0}), 0.0);
        assertEquals(2.0, FlutterAssets.chooseVariant(0.5, new double[] {2.0, 3.0}), 0.0);
    }
}
