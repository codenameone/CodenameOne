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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.GradientRaster;
import com.codename1.fxcompat.runtime.Renderer;
import com.codename1.fxcompat.runtime.Units;

import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderImage;
import javafx.scene.layout.BorderRepeat;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/// Gradients held against what their definition gives, and the pictures
/// of a background and of a border against where CSS places them.
public class GradientAndImageTest {

    private static final double EPS = 1e-9;

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        // A picture read from a file has a size only when its pixels are kept.
        HeadlessImplementation.pixelImages = true;
        Units.setScale(1);
    }

    @After
    public void tearDown() {
        Renderer.setTrace(null);
        Units.setScale(0);
        HeadlessImplementation.pixelImages = false;
    }

    private static Color at(Paint paint, double x, double y) {
        return GradientRaster.colorAt(paint, x, y, 0, 0, 100, 50);
    }

    private static void same(Color expected, Color actual) {
        assertEquals(expected.getRed(), actual.getRed(), 0.004);
        assertEquals(expected.getGreen(), actual.getGreen(), 0.004);
        assertEquals(expected.getBlue(), actual.getBlue(), 0.004);
        assertEquals(expected.getOpacity(), actual.getOpacity(), 0.004);
    }

    @Test
    public void aLinearGradientGivesAPointThePlaceOfItsFootOnTheLine() {
        Stop[] stops = {new Stop(0, Color.BLACK), new Stop(1, Color.WHITE)};
        // Proportional: the line runs over the whole width of 100.
        LinearGradient across = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, stops);
        same(Color.gray(0.25), at(across, 25, 40));
        same(Color.BLACK, at(across, -30, 10));
        same(Color.WHITE, at(across, 130, 10));
        // Absolute: from 10 to 30 whatever the rectangle is.
        LinearGradient part = new LinearGradient(10, 0, 30, 0, false, CycleMethod.NO_CYCLE, stops);
        same(Color.gray(0.5), at(part, 20, 0));
        same(Color.WHITE, at(part, 60, 0));
        // A diagonal: the foot of (10, 10) on the line from (0, 0) to
        // (20, 20) is half way.
        LinearGradient diagonal = new LinearGradient(0, 0, 20, 20, false, CycleMethod.NO_CYCLE, stops);
        same(Color.gray(0.5), at(diagonal, 20, 0));
        same(Color.gray(0.5), at(diagonal, 10, 10));
    }

    @Test
    public void theCycleMethodsBringAPlaceOutsideTheLineBackIn() {
        assertEquals(1, GradientRaster.cycle(1.3, CycleMethod.NO_CYCLE), EPS);
        assertEquals(0, GradientRaster.cycle(-0.3, CycleMethod.NO_CYCLE), EPS);
        assertEquals(0.3, GradientRaster.cycle(1.3, CycleMethod.REPEAT), EPS);
        assertEquals(0.7, GradientRaster.cycle(-0.3, CycleMethod.REPEAT), EPS);
        assertEquals(0.7, GradientRaster.cycle(1.3, CycleMethod.REFLECT), EPS);
        assertEquals(0.3, GradientRaster.cycle(-0.3, CycleMethod.REFLECT), EPS);
        assertEquals(0.3, GradientRaster.cycle(2.3, CycleMethod.REFLECT), EPS);
        Stop[] stops = {new Stop(0, Color.BLACK), new Stop(1, Color.WHITE)};
        LinearGradient reflected = new LinearGradient(0, 0, 20, 0, false, CycleMethod.REFLECT, stops);
        same(Color.gray(0.5), at(reflected, 30, 0));
        same(Color.BLACK, at(reflected, 40, 0));
        LinearGradient repeated = new LinearGradient(0, 0, 20, 0, false, CycleMethod.REPEAT, stops);
        same(Color.gray(0.25), at(repeated, 45, 0));
    }

    @Test
    public void aGradientOfSeveralStopsBlendsBetweenTheTwoAroundAPlace() {
        LinearGradient three = new LinearGradient(0, 0, 100, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED), new Stop(0.5, Color.LIME), new Stop(1, Color.BLUE));
        same(Color.RED, at(three, 0, 0));
        same(Color.color(0.5, 0.5, 0), at(three, 25, 0));
        same(Color.LIME, at(three, 50, 0));
        same(Color.color(0, 0.2, 0.8), at(three, 90, 0));
    }

    @Test
    public void aRadialGradientMeasuresAlongTheRayFromItsFocus() {
        Stop[] stops = {new Stop(0, Color.BLACK), new Stop(1, Color.WHITE)};
        // No focus: the distance from the centre over the radius.
        RadialGradient plain = new RadialGradient(0, 0, 50, 25, 20, false, CycleMethod.NO_CYCLE, stops);
        same(Color.BLACK, at(plain, 50, 25));
        same(Color.gray(0.5), at(plain, 60, 25));
        same(Color.gray(0.5), at(plain, 50, 15));
        same(Color.WHITE, at(plain, 90, 25));
        // A focus half way to the right edge of the circle: the ray to the
        // right is 10 long, the ray to the left 30.
        RadialGradient focused = new RadialGradient(0, 0.5, 50, 25, 20, false, CycleMethod.NO_CYCLE, stops);
        same(Color.BLACK, at(focused, 60, 25));
        same(Color.gray(0.5), at(focused, 65, 25));
        same(Color.gray(0.5), at(focused, 45, 25));
        same(Color.WHITE, at(focused, 30, 25));
        // The angle turns the focus: a quarter turn puts it below the
        // centre, y growing downwards.
        RadialGradient below = new RadialGradient(90, 0.5, 50, 25, 20, false, CycleMethod.NO_CYCLE, stops);
        same(Color.BLACK, at(below, 50, 35));
        same(Color.gray(0.5), at(below, 50, 40));
        // Past the circle a repeat starts over.
        RadialGradient rings = new RadialGradient(0, 0, 50, 25, 20, false, CycleMethod.REPEAT, stops);
        same(Color.gray(0.25), at(rings, 75, 25));
    }

    @Test
    public void aProportionalRadialGradientIsACircleOfTheUnitSquareStretchedOverTheRectangle() {
        Stop[] stops = {new Stop(0, Color.BLACK), new Stop(1, Color.WHITE)};
        RadialGradient g = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE, stops);
        // Over 100 by 50 the circle is an ellipse with radii 50 and 25.
        same(Color.BLACK, at(g, 50, 25));
        same(Color.gray(0.5), at(g, 75, 25));
        same(Color.gray(0.5), at(g, 50, 37.5));
        same(Color.WHITE, at(g, 50, 50));
    }

    // ------------------------------------------------------------ pictures

    private static List<double[]> images(Region region) {
        final List<double[]> out = new ArrayList<double[]>();
        Renderer.setTrace(new Renderer.Trace() {
            @Override
            public void drawn(String operation, double[] deviceBounds, Paint paint, String text) {
                if ("image".equals(operation)) {
                    out.add(deviceBounds);
                }
            }
        });
        region.cn1Paint(new Renderer(com.codename1.ui.Image.createImage(400, 400, 0).getGraphics(), 0, 0));
        Renderer.setTrace(null);
        return out;
    }

    private static Region sized(double w, double h) {
        Pane pane = new Pane();
        pane.resize(w, h);
        return pane;
    }

    private static void box(double x, double y, double w, double h, double[] bounds) {
        assertEquals(x, bounds[0], 0.01);
        assertEquals(y, bounds[1], 0.01);
        assertEquals(x + w, bounds[2], 0.01);
        assertEquals(y + h, bounds[3], 0.01);
    }

    @Test
    public void aBackgroundImageIsPlacedSizedAndRepeatedAsItSays() {
        WritableImage picture = new WritableImage(20, 10);
        Region region = sized(100, 50);
        // One copy in the middle.
        region.setBackground(new Background(new BackgroundImage(picture, BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
        List<double[]> drawn = images(region);
        assertEquals(1, drawn.size());
        box(40, 20, 20, 10, drawn.get(0));
        // Ten from the right edge and five from the bottom.
        region.setBackground(new Background(new BackgroundImage(picture, BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT, new BackgroundPosition(Side.RIGHT, 10, false, Side.BOTTOM, 5, false),
                null)));
        box(70, 35, 20, 10, images(region).get(0));
        // Repeated both ways from the corner: five across, five down.
        region.setBackground(new Background(new BackgroundImage(picture, null, null, null, null)));
        assertEquals(25, images(region).size());
        // Across only.
        region.setBackground(new Background(new BackgroundImage(picture, BackgroundRepeat.REPEAT,
                BackgroundRepeat.NO_REPEAT, null, null)));
        assertEquals(5, images(region).size());
        // Covering: the smallest size that leaves nothing bare.
        region.setBackground(new Background(new BackgroundImage(picture, BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT, null,
                new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, true, true, false, true))));
        box(0, 0, 100, 50, images(region).get(0));
        // Half the width, and the height that keeps the shape.
        region.setBackground(new Background(new BackgroundImage(picture, BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT, null,
                new BackgroundSize(0.5, BackgroundSize.AUTO, true, true, false, false))));
        box(0, 0, 50, 25, images(region).get(0));
        // Rounded: 30 wide in 100 is three copies of a third each.
        region.setBackground(new Background(new BackgroundImage(picture, BackgroundRepeat.ROUND,
                BackgroundRepeat.NO_REPEAT, null, new BackgroundSize(30, 10, false, false, false, false))));
        drawn = images(region);
        assertEquals(3, drawn.size());
        assertEquals(100 / 3.0, drawn.get(0)[2] - drawn.get(0)[0], 1);
    }

    @Test
    public void aBorderImageIsCutInNineAndDrawnAroundTheRegion() {
        WritableImage picture = new WritableImage(30, 30);
        Region region = sized(100, 60);
        region.setBorder(new Border(new BorderImage(picture, new BorderWidths(10), Insets.EMPTY,
                new BorderWidths(10), false, BorderRepeat.STRETCH, BorderRepeat.STRETCH)));
        List<double[]> drawn = images(region);
        assertEquals("four sides and four corners", 8, drawn.size());
        box(10, 0, 80, 10, drawn.get(0));
        box(10, 50, 80, 10, drawn.get(1));
        box(0, 10, 10, 40, drawn.get(2));
        box(90, 10, 10, 40, drawn.get(3));
        box(0, 0, 10, 10, drawn.get(4));
        box(90, 50, 10, 10, drawn.get(7));
        assertEquals("the border takes its width from the inside", 10, region.getInsets().getLeft(), EPS);

        // Filled, the middle is drawn too, under the rest.
        region.setBorder(new Border(new BorderImage(picture, new BorderWidths(10), Insets.EMPTY,
                new BorderWidths(10), true, BorderRepeat.STRETCH, BorderRepeat.STRETCH)));
        drawn = images(region);
        assertEquals(9, drawn.size());
        box(10, 10, 80, 40, drawn.get(0));

        // Repeated, a side is copies of its piece at the thickness of the
        // border, 10 long in 80. One copy is centred on the side, as CSS
        // has it, so seven whole ones and a half at each corner: nine.
        region.setBorder(new Border(new BorderImage(picture, new BorderWidths(10), Insets.EMPTY,
                new BorderWidths(10), false, BorderRepeat.REPEAT, BorderRepeat.STRETCH)));
        assertEquals(9 + 9 + 2 + 4, images(region).size());
        // Rounded, a whole number fits: eight.
        region.setBorder(new Border(new BorderImage(picture, new BorderWidths(10), Insets.EMPTY,
                new BorderWidths(10), false, BorderRepeat.ROUND, BorderRepeat.STRETCH)));
        assertEquals(8 + 8 + 2 + 4, images(region).size());
    }

    @Test
    public void aStyleNamesThePicturesOfABackgroundAndOfABorder() throws Exception {
        // The picture is made here and put where the resources of the
        // tests are read from; nothing is kept in the tree.
        File classes = new File(GradientAndImageTest.class.getResource("/cn1-hooks.txt").toURI()).getParentFile();
        java.awt.image.BufferedImage made = new java.awt.image.BufferedImage(20, 10,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        javax.imageio.ImageIO.write(made, "png", new File(classes, "style-picture.png"));

        Pane pane = new Pane();
        pane.setStyle("-fx-background-image: url('style-picture.png'); -fx-background-repeat: no-repeat;"
                + " -fx-background-position: right 10px bottom 5px;");
        Scene scene = new Scene(new Group(pane), 200, 200);
        scene.cn1Layout(200, 200);
        Background bg = pane.getBackground();
        assertNotNull(bg);
        assertEquals(1, bg.getImages().size());
        BackgroundImage image = bg.getImages().get(0);
        assertEquals(20, image.getImage().getWidth(), EPS);
        assertEquals(BackgroundRepeat.NO_REPEAT, image.getRepeatX());
        assertEquals(Side.RIGHT, image.getPosition().getHorizontalSide());
        assertEquals(10, image.getPosition().getHorizontalPosition(), EPS);
        assertEquals(Side.BOTTOM, image.getPosition().getVerticalSide());
        assertEquals(5, image.getPosition().getVerticalPosition(), EPS);

        pane.setStyle("-fx-background-image: url('style-picture.png'); -fx-background-repeat: repeat-x;"
                + " -fx-background-position: center; -fx-background-size: 50% auto;");
        scene.cn1Layout(200, 200);
        image = pane.getBackground().getImages().get(0);
        assertEquals(BackgroundRepeat.REPEAT, image.getRepeatX());
        assertEquals(BackgroundRepeat.NO_REPEAT, image.getRepeatY());
        assertEquals(0.5, image.getPosition().getHorizontalPosition(), EPS);
        assertTrue(image.getPosition().isVerticalAsPercentage());
        assertEquals(0.5, image.getPosition().getVerticalPosition(), EPS);
        assertEquals(0.5, image.getSize().getWidth(), EPS);
        assertTrue(image.getSize().isWidthAsPercentage());
        assertEquals(BackgroundSize.AUTO, image.getSize().getHeight(), EPS);

        pane.setStyle("-fx-border-image-source: url('style-picture.png'); -fx-border-image-slice: 3 4 fill;"
                + " -fx-border-image-width: 6; -fx-border-image-repeat: round stretch;"
                + " -fx-border-image-insets: 2;");
        scene.cn1Layout(200, 200);
        assertTrue("the background picture went with its style", pane.getBackground() == null
                || pane.getBackground().getImages().isEmpty());
        Border border = pane.getBorder();
        assertNotNull(border);
        assertEquals(1, border.getImages().size());
        BorderImage edge = border.getImages().get(0);
        assertTrue(edge.isFilled());
        assertEquals(3, edge.getSlices().getTop(), EPS);
        assertEquals(4, edge.getSlices().getRight(), EPS);
        assertEquals(3, edge.getSlices().getBottom(), EPS);
        assertEquals(4, edge.getSlices().getLeft(), EPS);
        assertEquals(6, edge.getWidths().getLeft(), EPS);
        assertEquals(BorderRepeat.ROUND, edge.getRepeatX());
        assertEquals(BorderRepeat.STRETCH, edge.getRepeatY());
        assertEquals(2, edge.getInsets().getTop(), EPS);
        assertEquals("insets and width together", 8, pane.getInsets().getTop(), EPS);
    }

    // ------------------------------------------------------------ direction

    private static String arabic(int... letters) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < letters.length; i++) {
            out.append((char) letters[i]);
        }
        return out.toString();
    }

    @Test
    public void aFlowShowsRunsAgainstTheLineInReverse() {
        // "He said <first>" in one text and " <second> to me." in the
        // next: read, the first Arabic word comes before the second;
        // seen, the two are one stretch that goes right to left, so the
        // second is on the left of the first.
        String first = arabic(0x627, 0x644, 0x633);
        String second = arabic(0x639, 0x644, 0x64a);
        javafx.scene.text.Text one = new javafx.scene.text.Text("He said " + first);
        javafx.scene.text.Text two = new javafx.scene.text.Text(" " + second + " to me.");
        javafx.scene.text.TextFlow flow = new javafx.scene.text.TextFlow(one, two);
        Scene scene = new Scene(new Group(flow), 600, 200);
        scene.cn1Layout(600, 200);

        final List<String> texts = new ArrayList<String>();
        final List<double[]> where = new ArrayList<double[]>();
        Renderer.setTrace(new Renderer.Trace() {
            @Override
            public void drawn(String operation, double[] deviceBounds, Paint paint, String text) {
                if ("text".equals(operation)) {
                    texts.add(text);
                    where.add(deviceBounds);
                }
            }
        });
        Renderer renderer = new Renderer(com.codename1.ui.Image.createImage(600, 200, 0).getGraphics(), 0, 0);
        renderer.translate(one.getLayoutX(), one.getLayoutY());
        one.cn1Paint(renderer);
        renderer = new Renderer(com.codename1.ui.Image.createImage(600, 200, 0).getGraphics(), 0, 0);
        renderer.translate(two.getLayoutX(), two.getLayoutY());
        two.cn1Paint(renderer);
        Renderer.setTrace(null);

        assertEquals(4, texts.size());
        double english = where.get(texts.indexOf("He said "))[0];
        double firstWord = where.get(texts.indexOf(first))[0];
        double secondWord = where.get(texts.indexOf(" " + second))[0];
        double tail = where.get(texts.indexOf(" to me."))[0];
        assertTrue("the line starts with its left-to-right start", english < secondWord);
        assertTrue("the second word is seen before the first", secondWord < firstWord);
        assertTrue("and the line ends with its left-to-right end", firstWord < tail);

        // A line with no right-to-left text is drawn as it was.
        javafx.scene.text.Text plain = new javafx.scene.text.Text("plain ");
        javafx.scene.text.Text words = new javafx.scene.text.Text("words");
        javafx.scene.text.TextFlow simple = new javafx.scene.text.TextFlow(plain, words);
        new Scene(new Group(simple), 600, 200).cn1Layout(600, 200);
        assertTrue(plain.getLayoutX() < words.getLayoutX());
    }
}
