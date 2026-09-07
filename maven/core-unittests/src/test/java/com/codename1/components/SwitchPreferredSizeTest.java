/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A Switch measures itself ARITHMETICALLY: asking for its images to read their
 * dimensions creates them, and creating the thumb runs a gaussian blur. Every
 * component on a screen is measured during layout, including the parts of it
 * that are off-screen and never painted, so a switch nobody sees was paying for
 * its artwork on the first frame.
 *
 * <p>The arithmetic and the artwork therefore have to agree, and nothing else in
 * the class makes them: this test is the link. If {@code createRoundThumbImage}
 * or {@code createRoundRectTrackImage} changes shape, this fails rather than the
 * switch silently laying out at the wrong size.</p>
 */
class SwitchPreferredSizeTest extends UITestBase {

    /** The size the images would have produced, which is what this used to return. */
    private static Dimension fromArtwork(Switch sw) {
        Image thumb = sw.currentThumbImageForTest();
        Image trackOn = sw.currentTrackOnImageForTest();
        Image trackOff = sw.currentTrackOffImageForTest();
        return new Dimension(
                sw.getStyle().getHorizontalPadding() + Math.max(thumb.getWidth(),
                        Math.max(trackOn.getWidth(), trackOff.getWidth())),
                sw.getStyle().getVerticalPadding() + Math.max(thumb.getHeight(),
                        Math.max(trackOn.getHeight(), trackOff.getHeight())));
    }

    private static void assertMeasuresLikeItsArtwork(Switch sw) {
        Dimension measured = sw.getPreferredSize();
        Dimension drawn = fromArtwork(sw);
        assertEquals(drawn.getWidth(), measured.getWidth(), "width");
        assertEquals(drawn.getHeight(), measured.getHeight(), "height");
    }

    @FormTest
    void anOffSwitchMeasuresLikeItsArtwork() {
        assertMeasuresLikeItsArtwork(new Switch());
    }

    @FormTest
    void anOnSwitchMeasuresLikeItsArtwork() {
        Switch sw = new Switch();
        sw.setValue(true);
        assertMeasuresLikeItsArtwork(sw);
    }

    @FormTest
    void aDisabledSwitchMeasuresLikeItsArtwork() {
        Switch sw = new Switch();
        sw.setEnabled(false);
        assertMeasuresLikeItsArtwork(sw);
    }

    @FormTest
    void measuringDoesNotRasteriseTheArtwork() {
        Switch sw = new Switch();
        sw.getPreferredSize();
        org.junit.jupiter.api.Assertions.assertFalse(sw.hasRasterisedArtworkForTest(),
                "measuring a Switch must not build its images");
    }
}
