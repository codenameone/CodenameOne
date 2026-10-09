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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.FrameClock;

import javafx.animation.Animation;
import javafx.animation.FillTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PathTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.StrokeTransition;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/// The transitions of a shape, and the animation of a game move as the
/// game writes it, on the manual frame clock: every frame has a stated
/// time, so what is expected is exact.
public class ShapeTransitionTest {

    private static final double EXACT = 1e-9;

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private final List<String> log = new ArrayList<String>();

    @BeforeClass
    public static void display() {
        HeadlessImplementation.install();
    }

    @Before
    public void setUp() {
        FrameClock.reset();
        FrameClock.setManual(true);
    }

    @After
    public void tearDown() {
        FrameClock.reset();
    }

    private static Duration ms(double millis) {
        return Duration.millis(millis);
    }

    private static long now() {
        return FrameClock.nowNanos() / 1000000L;
    }

    private static void assertColor(Color expected, Color actual) {
        assertEquals(expected.getRed(), actual.getRed(), EXACT);
        assertEquals(expected.getGreen(), actual.getGreen(), EXACT);
        assertEquals(expected.getBlue(), actual.getBlue(), EXACT);
        assertEquals(expected.getOpacity(), actual.getOpacity(), EXACT);
    }

    @Test
    public void fillGoesFromOneColourToTheOther() {
        Rectangle r = new Rectangle(10, 10, Color.GREEN);
        FillTransition fill = new FillTransition(ms(1000), r, Color.BLACK, Color.WHITE);
        fill.setInterpolator(Interpolator.LINEAR);
        assertSame(r, fill.getShape());
        assertEquals(ms(1000), fill.getCycleDuration());
        fill.play();
        FrameClock.advance(250);
        assertColor(Color.color(0.25, 0.25, 0.25), (Color) r.getFill());
        FrameClock.advance(750);
        // The end is the colour asked for itself.
        assertSame(Color.WHITE, r.getFill());
        assertEquals(Animation.Status.STOPPED, fill.getStatus());
    }

    @Test
    public void fillStartsAtWhatTheShapeHasAndEases() {
        Rectangle r = new Rectangle(10, 10, Color.color(0, 0, 0, 0));
        FillTransition fill = new FillTransition(ms(1000), r);
        fill.setToValue(Color.color(1, 0, 0, 1));
        assertSame(Interpolator.EASE_BOTH, fill.getInterpolator());
        fill.play();
        FrameClock.advance(100);
        // A tenth of the way in the eased curve has covered 3.125 * 0.1 * 0.1.
        assertColor(Color.color(0.03125, 0, 0, 0.03125), (Color) r.getFill());
        FrameClock.advance(400);
        assertColor(Color.color(0.5, 0, 0, 0.5), (Color) r.getFill());
    }

    @Test
    public void aFillThatIsNoColourIsLeftAlone() {
        LinearGradient gradient = new LinearGradient(0, 0, 1, 0, true, null, new Stop(0, Color.RED),
                new Stop(1, Color.BLUE));
        Rectangle r = new Rectangle(10, 10);
        r.setFill(gradient);
        FillTransition fill = new FillTransition(ms(100), r);
        fill.setToValue(Color.WHITE);
        fill.play();
        FrameClock.advance(50);
        assertSame(gradient, r.getFill());
        FrameClock.advance(50);
        assertSame(gradient, r.getFill());
        // Neither end given: nothing to do.
        r.setFill(Color.RED);
        FillTransition none = new FillTransition(ms(100), r);
        none.play();
        FrameClock.advance(100);
        assertSame(Color.RED, r.getFill());
    }

    @Test
    public void strokeGoesFromOneColourToTheOtherAndBack() {
        Circle c = new Circle(10);
        c.setStroke(Color.BLACK);
        StrokeTransition stroke = new StrokeTransition(ms(200), c, Color.BLACK, Color.WHITE);
        stroke.setInterpolator(Interpolator.LINEAR);
        stroke.setCycleCount(2);
        stroke.setAutoReverse(true);
        stroke.play();
        FrameClock.advance(100);
        assertColor(Color.color(0.5, 0.5, 0.5), (Color) c.getStroke());
        FrameClock.advance(150);
        assertColor(Color.color(0.75, 0.75, 0.75), (Color) c.getStroke());
        FrameClock.advance(150);
        assertSame(Color.BLACK, c.getStroke());
        assertEquals(Animation.Status.STOPPED, stroke.getStatus());
    }

    @Test
    public void aShapeTransitionInAParallelOneAnimatesItsShape() {
        Rectangle r = new Rectangle(10, 10, Color.BLACK);
        FillTransition fill = new FillTransition(ms(100));
        fill.setToValue(Color.WHITE);
        fill.setInterpolator(Interpolator.LINEAR);
        ParallelTransition both = new ParallelTransition(r, fill);
        both.play();
        FrameClock.advance(50);
        assertColor(Color.color(0.5, 0.5, 0.5), (Color) r.getFill());
        FrameClock.advance(50);
        assertSame(Color.WHITE, r.getFill());
    }

    @Test
    public void aNodeFollowsALineByItsCentre() {
        Rectangle r = new Rectangle(20, 10);
        Line line = new Line(100, 50, 300, 50);
        PathTransition along = new PathTransition(ms(1000), line, r);
        along.setInterpolator(Interpolator.LINEAR);
        assertSame(PathTransition.OrientationType.NONE, along.getOrientation());
        along.play();
        FrameClock.advance(0);
        assertEquals(100 - 10, r.getTranslateX(), EXACT);
        assertEquals(50 - 5, r.getTranslateY(), EXACT);
        FrameClock.advance(250);
        assertEquals(150 - 10, r.getTranslateX(), EXACT);
        FrameClock.advance(750);
        assertEquals(300 - 10, r.getTranslateX(), EXACT);
        assertEquals(50 - 5, r.getTranslateY(), EXACT);
        assertEquals(0, r.getRotate(), EXACT);
    }

    @Test
    public void aNodeFollowsACornerAtAnEvenSpeedAndTurnsWithIt() {
        Rectangle r = new Rectangle(10, 10);
        Path path = new Path(new MoveTo(0, 0), new LineTo(100, 0), new LineTo(100, 300));
        PathTransition along = new PathTransition(ms(400), path, r);
        along.setInterpolator(Interpolator.LINEAR);
        along.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
        along.play();
        // 400 units in 400 ms: one unit a millisecond, whatever the leg.
        FrameClock.advance(50);
        assertEquals(50 - 5, r.getTranslateX(), EXACT);
        assertEquals(-5, r.getTranslateY(), EXACT);
        assertEquals(0, r.getRotate(), EXACT);
        FrameClock.advance(150);
        assertEquals(100 - 5, r.getTranslateX(), EXACT);
        assertEquals(100 - 5, r.getTranslateY(), EXACT);
        assertEquals(90, r.getRotate(), 1e-6);
        FrameClock.advance(200);
        assertEquals(300 - 5, r.getTranslateY(), EXACT);
        assertEquals(Animation.Status.STOPPED, along.getStatus());
    }

    @Test
    public void aCurveIsFollowedWithinATenthOfAPixel() {
        Rectangle r = new Rectangle(0, 0);
        Circle circle = new Circle(100, 100, 50);
        PathTransition along = new PathTransition(ms(1000), circle, r);
        along.setInterpolator(Interpolator.LINEAR);
        along.play();
        for (int i = 0; i < 10; i++) {
            FrameClock.advance(100);
            double dx = r.getTranslateX() - 100;
            double dy = r.getTranslateY() - 100;
            assertEquals(50, Math.sqrt(dx * dx + dy * dy), 0.11);
        }
    }

    /// One move of a sliding tile game as such a game writes it: every
    /// tile slides for 65 ms, then the new tile grows for 125 ms while a
    /// merged one swells and settles in two steps of 80 ms. What matters
    /// is that every stage ends exactly on its end value and on time, with
    /// frames 16 ms apart that never land on a boundary.
    @Test
    public void aGameMoveSlidesThenGrowsAndEndsOnItsValues() {
        final Rectangle slider = new Rectangle(10, 10);
        final Rectangle born = new Rectangle(10, 10);
        final Rectangle merged = new Rectangle(10, 10);
        born.setScaleX(0);
        born.setScaleY(0);
        Timeline slide = new Timeline(new KeyFrame(ms(65),
                new KeyValue(slider.layoutXProperty(), 396, Interpolator.EASE_OUT),
                new KeyValue(slider.layoutYProperty(), 132, Interpolator.EASE_OUT)));
        final ScaleTransition grow = new ScaleTransition(ms(125), born);
        grow.setToX(1.0);
        grow.setToY(1.0);
        grow.setInterpolator(Interpolator.EASE_OUT);
        grow.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                log.add("grown@" + now());
            }
        });
        ScaleTransition up = new ScaleTransition(ms(80), merged);
        up.setToX(1.2);
        up.setToY(1.2);
        up.setInterpolator(Interpolator.EASE_IN);
        ScaleTransition down = new ScaleTransition(ms(80), merged);
        down.setToX(1.0);
        down.setToY(1.0);
        down.setInterpolator(Interpolator.EASE_OUT);
        final SequentialTransition pop = new SequentialTransition(up, down);
        pop.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                log.add("popped@" + now());
            }
        });
        final ParallelTransition after = new ParallelTransition(grow, pop);
        slide.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                log.add("slid@" + now());
                after.play();
            }
        });
        long start = now();
        slide.play();
        double last = 0;
        double peak = 0;
        for (int t = 16; t <= 320; t += 16) {
            FrameClock.advance(16);
            // A slide only ever moves forward.
            assertEquals(true, slider.getLayoutX() >= last);
            last = slider.getLayoutX();
            peak = Math.max(peak, merged.getScaleX());
        }
        assertEquals(396, slider.getLayoutX(), EXACT);
        assertEquals(132, slider.getLayoutY(), EXACT);
        assertEquals(1.0, born.getScaleX(), EXACT);
        assertEquals(1.0, born.getScaleY(), EXACT);
        assertEquals(1.0, merged.getScaleX(), EXACT);
        assertEquals(true, peak > 1.15);
        // The slide is over on the first frame at or after 65 ms, the growth 125 ms
        // after that frame and the pop 160 ms after it, each on the next frame.
        assertEquals("[slid@" + (start + 80) + ", grown@" + (start + 80 + 128) + ", popped@" + (start + 80 + 160) + "]",
                log.toString());
    }
}
