/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.Effects;
import com.codename1.fxcompat.runtime.Units;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Glow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/// A drop shadow and an inner shadow are drawn: the pixels of each, and
/// that a node's peer draws the picture and keeps it between paints.
public class ShadowEffectTest {

    private static final int W = 24;
    private static final int H = 24;

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(1);
    }

    @After
    public void tearDown() {
        HeadlessImplementation.recordDraws = false;
        HeadlessImplementation.draws.clear();
        List<javafx.stage.Window> open = new ArrayList<javafx.stage.Window>(javafx.stage.Window.getWindows());
        for (int i = 0; i < open.size(); i++) {
            open.get(i).hide();
        }
        Units.setScale(0);
    }

    /// An opaque red square from 8 to 15 on a transparent picture.
    private static int[] square() {
        int[] p = new int[W * H];
        for (int y = 8; y < 16; y++) {
            for (int x = 8; x < 16; x++) {
                p[y * W + x] = 0xffff0000;
            }
        }
        return p;
    }

    private static int alpha(int[] p, int x, int y) {
        return p[y * W + x] >>> 24;
    }

    @Test
    public void aDropShadowIsTheOutlineBlurredInTheShadowsColour() {
        int[] s = Effects.dropShadow(square(), W, H, 3, 3, 0, 0xff102030);
        for (int i = 0; i < s.length; i++) {
            assertEquals("the colour of pixel " + i, 0x102030, s[i] & 0xffffff);
        }
        assertTrue("dense under the middle: " + alpha(s, 11, 11), alpha(s, 11, 11) > 200);
        int justOutside = alpha(s, 6, 11);
        assertTrue("soft beyond the edge: " + justOutside, justOutside > 10 && justOutside < 200);
        assertTrue("fading with distance", alpha(s, 5, 11) < justOutside && alpha(s, 7, 11) > justOutside);
        assertEquals("nothing past the radius", 0, alpha(s, 2, 11));
        assertEquals("nothing in the corner", 0, alpha(s, 0, 0));
        // The same on every side.
        assertEquals(alpha(s, 6, 11), alpha(s, 17, 12));
        assertEquals(alpha(s, 6, 11), alpha(s, 11, 6));
        assertEquals(alpha(s, 6, 11), alpha(s, 12, 17));
    }

    @Test
    public void aSpreadStrengthensAndAColoursAlphaWeakensTheShadow() {
        int[] plain = Effects.dropShadow(square(), W, H, 3, 3, 0, 0xff000000);
        int[] spread = Effects.dropShadow(square(), W, H, 3, 3, 0.5, 0xff000000);
        int[] faint = Effects.dropShadow(square(), W, H, 3, 3, 0, 0x80000000);
        assertTrue(alpha(spread, 6, 11) >= 2 * alpha(plain, 6, 11) - 1);
        assertEquals(255, alpha(spread, 11, 11));
        assertTrue(alpha(faint, 11, 11) <= 128 && alpha(faint, 11, 11) > 100);
        // No radius: the outline itself, hard.
        int[] hard = Effects.dropShadow(square(), W, H, 0, 3, 0, 0xff000000);
        assertEquals(255, alpha(hard, 8, 8));
        assertEquals(0, alpha(hard, 7, 8));
    }

    @Test
    public void oneBoxPassIsAFlatAverage() {
        int[] s = Effects.dropShadow(square(), W, H, 2, 1, 0, 0xff000000);
        // A box of five by five around (7, 11) covers one column of the
        // square in all five rows: a fifth.
        assertEquals(255 / 5, alpha(s, 6, 11));
        assertEquals(0, alpha(s, 5, 11));
    }

    @Test
    public void anInnerShadowDarkensTheEdgeInsideAndNothingOutside() {
        int[] s = Effects.innerShadow(square(), W, H, 2, 3, 0, 0xff000000, 0, 0);
        assertEquals("nothing where the node is not", 0, alpha(s, 7, 11));
        assertEquals(0, alpha(s, 0, 0));
        assertTrue("dark at the edge: " + alpha(s, 8, 11), alpha(s, 8, 11) > 80);
        assertTrue("lighter further in", alpha(s, 10, 11) < alpha(s, 8, 11));
        assertEquals("clear in the middle of a large enough node", 0, alpha(s, 11, 11) > 40 ? 1 : 0);
        // Moved right, the shadow falls from the left edge.
        int[] moved = Effects.innerShadow(square(), W, H, 1, 1, 0, 0xff000000, 3, 0);
        assertTrue(alpha(moved, 9, 11) > 200);
        assertTrue(alpha(moved, 15, 11) < alpha(moved, 9, 11));
    }

    private static int images() {
        int n = 0;
        for (int i = 0; i < HeadlessImplementation.draws.size(); i++) {
            if ("drawImage".equals(HeadlessImplementation.draws.get(i)[0])) {
                n++;
            }
        }
        return n;
    }

    private static int paint() {
        HeadlessImplementation.draws.clear();
        HeadlessImplementation.recordDraws = true;
        Form f = Display.getInstance().getCurrent();
        f.revalidate();
        f.paintComponent(Image.createImage(HeadlessImplementation.WIDTH, HeadlessImplementation.HEIGHT).getGraphics());
        HeadlessImplementation.recordDraws = false;
        return images();
    }

    @Test
    public void aNodeDrawsItsShadowAndKeepsThePictureUntilSomethingUnderItChanges() {
        Rectangle inside = new Rectangle(5, 5, 30, 30);
        inside.setFill(Color.RED);
        Pane card = new Pane(inside);
        card.resizeRelocate(40, 40, 100, 60);
        Rectangle plain = new Rectangle(200, 10, 20, 20);
        Stage stage = new Stage();
        stage.setScene(new Scene(new Group(card, plain), 300, 200));
        stage.show();
        int without = paint();

        card.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.BLACK, 6, 0, 2, 3));
        int made = Effects.captureCount();
        assertEquals("one picture more is drawn, the shadow", without + 1, paint());
        assertEquals("the node was painted once to make it", made + 1, Effects.captureCount());
        assertEquals(without + 1, paint());
        assertEquals("and not again for a second paint", made + 1, Effects.captureCount());

        // Something inside the node changed: its outline may have.
        inside.setWidth(50);
        paint();
        assertEquals(made + 2, Effects.captureCount());
        // A value of the effect changed.
        ((DropShadow) card.getEffect()).setRadius(9);
        paint();
        assertEquals(made + 3, Effects.captureCount());
        // An unrelated node does not disturb it.
        plain.setFill(Color.GREEN);
        paint();
        assertEquals(made + 3, Effects.captureCount());

        // Both at once: one behind and one over.
        InnerShadow inner = new InnerShadow(4, Color.BLUE);
        inner.setInput(new DropShadow(5, Color.GRAY));
        card.setEffect(inner);
        assertEquals(without + 2, paint());

        // An effect that is not drawn costs nothing, and none restores.
        card.setEffect(new Glow(0.5));
        assertEquals(without, paint());
        card.setEffect(null);
        assertEquals(without, paint());
        assertNull(card.cn1EffectCache());
    }

    @Test
    public void aStyleSheetsEffectIsADropShadowOrAnInnerShadow() {
        Pane card = new Pane();
        Stage stage = new Stage();
        stage.setScene(new Scene(new Group(card), 300, 200));
        stage.show();
        card.setStyle("-fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.5) , 10, 0.25 , 2 , 4 );");
        card.applyCss();
        Effect e = card.getEffect();
        assertTrue(String.valueOf(e), e instanceof DropShadow);
        DropShadow d = (DropShadow) e;
        assertEquals(10, d.getRadius(), 0);
        assertEquals(0.25, d.getSpread(), 0);
        assertEquals(2, d.getOffsetX(), 0);
        assertEquals(4, d.getOffsetY(), 0);
        assertEquals(0.5, d.getColor().getOpacity(), 0.01);
        assertEquals(BlurType.THREE_PASS_BOX, d.getBlurType());

        card.setStyle("-fx-effect: innershadow(one-pass-box, derive(#804020, -20%), 3, 0, 0, 1);");
        card.applyCss();
        assertTrue(card.getEffect() instanceof InnerShadow);
        assertEquals(BlurType.ONE_PASS_BOX, ((InnerShadow) card.getEffect()).getBlurType());

        card.setStyle("");
        card.applyCss();
        assertNull("the effect goes with the style", card.getEffect());
    }
}
