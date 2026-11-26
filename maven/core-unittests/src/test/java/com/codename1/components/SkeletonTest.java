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
package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.CommonProgressAnimations;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

class SkeletonTest extends UITestBase {

    @FormTest
    void factoriesUseTheSkeletonUiidAndShimmerByDefault() {
        Skeleton[] all = {new Skeleton(), Skeleton.rows(3), Skeleton.text(2),
                Skeleton.circle(8), Skeleton.rect(20, 10), Skeleton.card()};
        for (Skeleton s : all) {
            assertEquals("Skeleton", s.getUIID());
            assertTrue(s.isShimmer());
            assertFalse(s.isFocusable());
            assertTrue(s.getPreferredH() > 0);
        }
    }

    @FormTest
    void moreRowsAreTaller() {
        assertTrue(Skeleton.rows(4).getPreferredH() > Skeleton.rows(2).getPreferredH());
        assertTrue(Skeleton.text(4).getPreferredH() > Skeleton.text(2).getPreferredH());
    }

    @FormTest
    void showMasksAndHideRestoresTheComponent() {
        Form f = new Form(BoxLayout.y());
        Label before = new Label("before");
        Label target = new Label("target");
        f.add(before).add(target);
        f.show();
        Skeleton s = Skeleton.show(target);
        Container parent = s.getParent();
        assertSame(f.getContentPane(), parent);
        assertNull(target.getParent());
        assertEquals(1, parent.getComponentIndex(s));
        assertSame(s, Skeleton.show(target), "a second show returns the placeholder already up");
        assertSame(s, CommonProgressAnimations.ProgressAnimation.getProgressAnimation(target));
        Skeleton.hide(target);
        assertSame(parent, target.getParent());
        assertEquals(1, parent.getComponentIndex(target));
        assertNull(s.getParent());
        Skeleton.hide(target);
    }

    @FormTest
    void shimmerAnimatesOnlyWhenOn() {
        Skeleton s = Skeleton.rows(2);
        assertTrue(s.animate());
        assertFalse(s.shimmer(false).animate());
    }

    @FormTest
    void paintsInBothDirections() {
        Form f = new Form(BoxLayout.y());
        Skeleton[] all = {Skeleton.rows(3), Skeleton.text(3), Skeleton.circle(8),
                Skeleton.rect(0, 10), Skeleton.card(), new Skeleton()};
        for (Skeleton s : all) {
            f.add(s);
        }
        f.show();
        Image img = Image.createImage(f.getWidth(), f.getHeight(), 0xffffffff);
        f.paintComponent(img.getGraphics(), true);
        for (Skeleton s : all) {
            s.setRTL(true);
        }
        f.paintComponent(img.getGraphics(), true);
    }
}
