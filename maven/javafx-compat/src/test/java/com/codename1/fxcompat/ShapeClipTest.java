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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.codename1.fxcompat.runtime.FxPath;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import org.junit.Test;

/// A clip that is a shape clips to the shape's outline, not to its box.
public class ShapeClipTest {

    @Test
    public void aCircleClipLeavesTheCornersOut() {
        ImageView photo = new ImageView();
        photo.setClip(new Circle(50, 50, 50));
        FxPath clip = photo.cn1ClipPath();
        assertTrue(clip.contains(50, 50));
        assertTrue(clip.contains(50, 2));
        assertFalse("the corner of the box lies outside the circle", clip.contains(4, 4));
        assertFalse(clip.contains(96, 96));
    }

    @Test
    public void theClipShapeIsPlacedByItsOwnTranslation() {
        Pane node = new Pane();
        Rectangle r = new Rectangle(0, 0, 20, 20);
        r.setArcWidth(20);
        r.setArcHeight(20);
        r.setLayoutX(30);
        r.setTranslateY(10);
        node.setClip(r);
        FxPath clip = node.cn1ClipPath();
        assertTrue(clip.contains(40, 20));
        assertFalse("outside the rounded corner", clip.contains(31, 11));
        assertFalse("where the shape would be without its translation", clip.contains(10, 10));
    }
}
