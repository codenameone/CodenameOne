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
package com.codename1.fxcompat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.codename1.fxcompat.runtime.FxPath;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;

/// A rounded clip is narrowed by every rectangle painted inside it, and
/// Codename One's intersection threw when a corner ended a fraction of a
/// pixel from the rectangle's edge. The numbers are those an iOS build
/// logged when it went down on the showcases list.
public class ClipSnapTest {

    private static FxPath corner() {
        FxPath p = new FxPath();
        p.moveTo(300, 2300);
        p.lineTo(300, 2380.125);
        p.lineTo(102.36949157714844, 2380.125);
        p.curveTo(98.39994049072266, 2380.125, 95.18199157714844, 2376.906982421875,
                95.18199157714844, 2372.9375);
        p.lineTo(95.18199157714844, 2300);
        p.closePath();
        return p;
    }

    @Test
    public void aSnappedClipNarrowsWhereTheExactOneThrew() {
        double[] identity = {1, 0, 0, 1, 0, 0};
        Rectangle rect = new Rectangle(44, 2222, 292, 151);
        try {
            corner().toDevice(identity).intersection(rect);
            fail("the exact path no longer throws; the snapping in Renderer.clip can go");
        } catch (RuntimeException expected) {
            // The core bug the snapping works around.
        }
        GeneralPath snapped = corner().toDevice(identity, true);
        assertNotNull(snapped.intersection(rect));
    }
}
