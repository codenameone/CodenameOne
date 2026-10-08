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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
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
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolatable;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/// Animation on the manual frame clock: every test states the time of
/// each frame, so the values it expects are exact and nothing sleeps.
public class AnimationTest {

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

    private EventHandler<ActionEvent> note(final String what) {
        return new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                log.add(what);
            }
        };
    }

    private static Timeline line(DoubleProperty x, double to, double millis) {
        return new Timeline(new KeyFrame(ms(millis), new KeyValue(x, to)));
    }

    // ---- interpolators ----

    @Test
    public void interpolatorCurves() {
        assertEquals(25.0, Interpolator.LINEAR.interpolate(0.0, 100.0, 0.25), EXACT);
        assertEquals(0.0, Interpolator.EASE_BOTH.interpolate(0.0, 1.0, 0.0), EXACT);
        assertEquals(1.0, Interpolator.EASE_BOTH.interpolate(0.0, 1.0, 1.0), EXACT);
        assertEquals(0.5, Interpolator.EASE_BOTH.interpolate(0.0, 1.0, 0.5), EXACT);
        // Accelerating: 3.125 t^2 over the first fifth.
        assertEquals(0.03125, Interpolator.EASE_BOTH.interpolate(0.0, 1.0, 0.1), EXACT);
        assertEquals(0.96875, Interpolator.EASE_BOTH.interpolate(0.0, 1.0, 0.9), EXACT);
        // The two parts meet at the same value.
        assertEquals(0.125, Interpolator.EASE_BOTH.interpolate(0.0, 1.0, 0.2), EXACT);
        assertEquals(25.0 / 900.0, Interpolator.EASE_IN.interpolate(0.0, 1.0, 0.1), EXACT);
        assertEquals(1.0, Interpolator.EASE_IN.interpolate(0.0, 1.0, 1.0), EXACT);
        assertEquals(5.0 / 9.0, Interpolator.EASE_OUT.interpolate(0.0, 1.0, 0.5), EXACT);
        assertEquals(1.0, Interpolator.EASE_OUT.interpolate(0.0, 1.0, 1.0), EXACT);
        assertEquals(0.0, Interpolator.DISCRETE.interpolate(0.0, 1.0, 0.999), EXACT);
        assertEquals(1.0, Interpolator.DISCRETE.interpolate(0.0, 1.0, 1.0), EXACT);
    }

    @Test
    public void splineMidpointByHand() {
        // x(s) = 3(1-s)^2 s x1 + 3(1-s) s^2 x2 + s^3. With x1 + x2 = 1 the
        // curve is at x = 0.5 for s = 0.5: 3/8 * 1 + 1/8. There
        // y = 3/8 * (0.2 + 0.4) + 1/8 = 0.35.
        Interpolator spline = Interpolator.SPLINE(0.3, 0.2, 0.7, 0.4);
        assertEquals(0.35, spline.interpolate(0.0, 1.0, 0.5), 1e-9);
        assertEquals(0.0, spline.interpolate(0.0, 1.0, 0.0), 1e-9);
        assertEquals(1.0, spline.interpolate(0.0, 1.0, 1.0), 1e-9);
        try {
            Interpolator.SPLINE(1.5, 0, 0, 0);
            fail("a control point outside 0..1 is refused");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        DoubleProperty x = new SimpleDoubleProperty(0);
        Timeline t = new Timeline(new KeyFrame(ms(1000), new KeyValue(x, 100.0, spline)));
        t.play();
        FrameClock.advance(500);
        assertEquals(35.0, x.get(), 1e-6);
    }

    /// A value that blends itself.
    private static final class Span implements Interpolatable<Span> {
        final double length;

        Span(double length) {
            this.length = length;
        }

        @Override
        public Span interpolate(Span endValue, double t) {
            return new Span(length + (endValue.length - length) * t);
        }
    }

    @Test
    public void whatInterpolatesAndWhatSwitchesAtTheEnd() {
        Interpolator i = Interpolator.LINEAR;
        assertEquals(Integer.valueOf(3), i.interpolate(Integer.valueOf(0), Integer.valueOf(10), 0.25));
        assertEquals(Long.valueOf(5), i.interpolate(Long.valueOf(0), Long.valueOf(10), 0.5));
        assertEquals(Double.valueOf(2.5), i.interpolate(Integer.valueOf(0), Double.valueOf(10), 0.25));
        assertEquals(3, i.interpolate(0, 10, 0.25));
        assertEquals(5L, i.interpolate(0L, 10L, 0.5));
        Object blended = i.interpolate(new Span(10), new Span(20), 0.5);
        assertTrue(blended instanceof Span);
        assertEquals(15.0, ((Span) blended).length, EXACT);
        assertEquals("a", i.interpolate("a", "b", 0.99));
        assertEquals("b", i.interpolate("a", "b", 1.0));
        assertFalse(i.interpolate(false, true, 0.99));
        assertTrue(i.interpolate(false, true, 1.0));

        ObjectProperty<String> text = new SimpleObjectProperty<String>("before");
        IntegerProperty count = new SimpleIntegerProperty(0);
        BooleanProperty flag = new SimpleBooleanProperty(false);
        Timeline t = new Timeline(new KeyFrame(ms(100), new KeyValue(text, "after"), new KeyValue(count, 10),
                new KeyValue(flag, true)));
        t.play();
        FrameClock.advance(50);
        assertEquals("before", text.get());
        assertEquals(5, count.get());
        assertFalse(flag.get());
        FrameClock.advance(50);
        assertEquals("after", text.get());
        assertEquals(10, count.get());
        assertTrue(flag.get());
    }

    // ---- timeline ----

    @Test
    public void linearValuesAtChosenTimes() {
        DoubleProperty x = new SimpleDoubleProperty(10);
        Timeline t = line(x, 110, 1000);
        t.setOnFinished(note("finished"));
        assertEquals(ms(1000), t.getCycleDuration());
        assertEquals(ms(1000), t.getTotalDuration());
        assertSame(Animation.Status.STOPPED, t.getStatus());
        t.play();
        assertSame(Animation.Status.RUNNING, t.getStatus());
        assertEquals(1.0, t.getCurrentRate(), EXACT);
        assertEquals(10.0, x.get(), EXACT);
        FrameClock.advance(250);
        assertEquals(35.0, x.get(), EXACT);
        assertEquals(ms(250), t.getCurrentTime());
        FrameClock.advance(250);
        assertEquals(60.0, x.get(), EXACT);
        assertTrue(log.isEmpty());
        FrameClock.advance(500);
        assertEquals(110.0, x.get(), EXACT);
        assertSame(Animation.Status.STOPPED, t.getStatus());
        assertEquals(0.0, t.getCurrentRate(), EXACT);
        assertEquals(Arrays.asList("finished"), log);
        assertFalse("an idle clock has no receivers", FrameClock.isActive());
        FrameClock.advance(500);
        assertEquals(Arrays.asList("finished"), log);
    }

    @Test
    public void aKeyFrameAtZeroSetsTheStartOtherwiseItIsCapturedAtPlay() {
        DoubleProperty x = new SimpleDoubleProperty(5);
        DoubleProperty y = new SimpleDoubleProperty(7);
        Timeline t = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(x, 20.0)),
                new KeyFrame(ms(1000), new KeyValue(x, 40.0), new KeyValue(y, 200.0)));
        // Changed after the timeline was built, before it is played: this is
        // the value y starts from.
        y.set(100);
        t.play();
        y.set(-1);
        FrameClock.advance(500);
        assertEquals(30.0, x.get(), EXACT);
        assertEquals(150.0, y.get(), EXACT);
        FrameClock.advance(500);
        assertEquals(40.0, x.get(), EXACT);
        assertEquals(200.0, y.get(), EXACT);
    }

    @Test
    public void severalKeyFramesForOneTarget() {
        DoubleProperty x = new SimpleDoubleProperty(0);
        Timeline t = new Timeline(new KeyFrame(ms(100), new KeyValue(x, 100.0)),
                new KeyFrame(ms(300), new KeyValue(x, 0.0)));
        t.play();
        FrameClock.advance(50);
        assertEquals(50.0, x.get(), EXACT);
        FrameClock.advance(150);
        assertEquals(50.0, x.get(), EXACT);
        FrameClock.advance(50);
        assertEquals(25.0, x.get(), EXACT);
    }

    @Test
    public void keyFrameHandlersRunInOrderWhenAFrameJumpsPastSeveral() {
        final DoubleProperty x = new SimpleDoubleProperty(0);
        final List<Double> seen = new ArrayList<Double>();
        EventHandler<ActionEvent> record = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                seen.add(Double.valueOf(x.get()));
            }
        };
        // Deliberately not in time order.
        Timeline t = new Timeline(new KeyFrame(ms(300), note("c"), new KeyValue(x, 300.0)),
                new KeyFrame(ms(100), note("a")), new KeyFrame(ms(200), note("b")),
                new KeyFrame(Duration.ZERO, note("zero")), new KeyFrame(ms(100), record),
                new KeyFrame(ms(200), record));
        t.setOnFinished(note("finished"));
        t.play();
        assertTrue("nothing runs before the first frame", log.isEmpty());
        FrameClock.advance(1000);
        assertEquals(Arrays.asList("zero", "a", "b", "c", "finished"), log);
        // Each handler saw the targets as they are at its own key frame.
        assertEquals(Arrays.asList(Double.valueOf(100), Double.valueOf(200)), seen);
    }

    @Test
    public void keyFrameHandlersRunOncePerCycle() {
        DoubleProperty x = new SimpleDoubleProperty(0);
        Timeline t = new Timeline(new KeyFrame(Duration.ZERO, note("start")),
                new KeyFrame(ms(100), note("end"), new KeyValue(x, 100.0)));
        t.setCycleCount(3);
        t.setOnFinished(note("finished"));
        assertEquals(ms(300), t.getTotalDuration());
        t.play();
        FrameClock.advance(50);
        assertEquals(Arrays.asList("start"), log);
        FrameClock.advance(200);
        // 250 ms: two cycles over, half way through the third.
        assertEquals(Arrays.asList("start", "end", "start", "end", "start"), log);
        assertEquals(50.0, x.get(), EXACT);
        assertSame(Animation.Status.RUNNING, t.getStatus());
        FrameClock.advance(50);
        assertEquals(Arrays.asList("start", "end", "start", "end", "start", "end", "finished"), log);
        assertEquals(100.0, x.get(), EXACT);

        log.clear();
        t.play();
        FrameClock.advance(5000);
        assertEquals(Arrays.asList("start", "end", "start", "end", "start", "end", "finished"), log);
    }

    @Test
    public void autoReverseFlipsTheDirection() {
        DoubleProperty x = new SimpleDoubleProperty(0);
        Timeline t = new Timeline(new KeyFrame(ms(1000), note("end"), new KeyValue(x, 100.0)));
        t.setCycleCount(2);
        t.setAutoReverse(true);
        t.setOnFinished(note("finished"));
        t.play();
        FrameClock.advance(500);
        assertEquals(50.0, x.get(), EXACT);
        assertEquals(1.0, t.getCurrentRate(), EXACT);
        FrameClock.advance(750);
        // 1250 ms: a quarter of the way back.
        assertEquals(75.0, x.get(), EXACT);
        assertEquals(-1.0, t.getCurrentRate(), EXACT);
        assertEquals(ms(750), t.getCurrentTime());
        assertEquals("the key frame at the turning point runs once", Arrays.asList("end"), log);
        FrameClock.advance(500);
        assertEquals(25.0, x.get(), EXACT);
        FrameClock.advance(250);
        assertEquals(0.0, x.get(), EXACT);
        assertSame(Animation.Status.STOPPED, t.getStatus());
        assertEquals(Arrays.asList("end", "finished"), log);
    }

    @Test
    public void rateScalesAndANegativeRatePlaysBackwards() {
        DoubleProperty x = new SimpleDoubleProperty(0);
        Timeline t = line(x, 100, 1000);
        t.setOnFinished(note("finished"));
        t.setRate(2);
        t.play();
        FrameClock.advance(250);
        assertEquals(50.0, x.get(), EXACT);
        assertEquals(2.0, t.getCurrentRate(), EXACT);
        // Turned round in mid flight.
        t.setRate(-1);
        assertEquals(-1.0, t.getCurrentRate(), EXACT);
        FrameClock.advance(200);
        assertEquals(30.0, x.get(), EXACT);
        FrameClock.advance(300);
        assertEquals(0.0, x.get(), EXACT);
        assertSame(Animation.Status.STOPPED, t.getStatus());
        assertEquals(Arrays.asList("finished"), log);

        // From the end, backwards.
        log.clear();
        t.jumpTo("end");
        t.play();
        FrameClock.advance(250);
        assertEquals(75.0, x.get(), EXACT);
        FrameClock.advance(750);
        assertEquals(0.0, x.get(), EXACT);
        assertEquals(Arrays.asList("finished"), log);

        // Having finished, a backwards animation starts over from its end.
        log.clear();
        t.play();
        FrameClock.advance(500);
        assertEquals(50.0, x.get(), EXACT);
        assertTrue(log.isEmpty());
    }

    @Test
    public void pauseKeepsThePositionAndPlayResumes() {
        DoubleProperty x = new SimpleDoubleProperty(0);
        Timeline t = line(x, 100, 1000);
        t.play();
        FrameClock.advance(300);
        t.pause();
        assertSame(Animation.Status.PAUSED, t.getStatus());
        assertEquals(0.0, t.getCurrentRate(), EXACT);
        assertFalse(FrameClock.isActive());
        FrameClock.advance(5000);
        assertEquals(30.0, x.get(), EXACT);
        assertEquals(ms(300), t.getCurrentTime());
        t.play();
        assertSame(Animation.Status.RUNNING, t.getStatus());
        FrameClock.advance(200);
        assertEquals(50.0, x.get(), EXACT);
    }

    @Test
    public void stopResetsToTheStartAndDoesNotFinish() {
        DoubleProperty x = new SimpleDoubleProperty(0);
        Timeline t = line(x, 100, 1000);
        t.setOnFinished(note("finished"));
        t.play();
        FrameClock.advance(300);
        t.stop();
        assertSame(Animation.Status.STOPPED, t.getStatus());
        assertEquals(Duration.ZERO, t.getCurrentTime());
        assertEquals("the value stays where the animation left it", 30.0, x.get(), EXACT);
        FrameClock.advance(5000);
        assertTrue(log.isEmpty());
        assertEquals(30.0, x.get(), EXACT);
        // Played again it starts at the start, from the value the target has now.
        t.play();
        FrameClock.advance(500);
        assertEquals(65.0, x.get(), EXACT);
    }

    @Test
    public void jumpToAndCuePoints() {
        DoubleProperty x = new SimpleDoubleProperty(0);
        Timeline t = new Timeline(new KeyFrame(ms(250), "quarter", new KeyValue[0]),
                new KeyFrame(ms(1000), new KeyValue(x, 100.0)));
        assertEquals(ms(250), t.getCuePoints().get("quarter"));
        t.getCuePoints().put("most", ms(800));
        t.play();
        FrameClock.advance(100);
        t.jumpTo(ms(800));
        assertEquals("a running animation shows the position at once", 80.0, x.get(), EXACT);
        assertEquals(ms(800), t.getCurrentTime());
        FrameClock.advance(100);
        assertEquals(90.0, x.get(), EXACT);
        t.jumpTo(ms(-5));
        assertEquals(0.0, x.get(), EXACT);
        t.jumpTo(ms(99999));
        assertEquals(100.0, x.get(), EXACT);
        t.stop();

        x.set(0);
        t.playFrom("quarter");
        assertEquals(ms(250), t.getCurrentTime());
        FrameClock.advance(250);
        assertEquals(50.0, x.get(), EXACT);
        t.jumpTo("most");
        assertEquals(80.0, x.get(), EXACT);
        t.jumpTo("no such cue point");
        assertEquals(80.0, x.get(), EXACT);
        t.playFrom(ms(500));
        FrameClock.advance(100);
        assertEquals(60.0, x.get(), EXACT);
        t.setRate(-3);
        t.playFromStart();
        assertEquals(3.0, t.getRate(), EXACT);
        // 300 ms in at three times the speed, starting from the 60 it had.
        FrameClock.advance(100);
        assertEquals(72.0, x.get(), EXACT);
    }

    @Test
    public void indefiniteNeverFinishes() {
        DoubleProperty x = new SimpleDoubleProperty(0);
        Timeline t = new Timeline(new KeyFrame(ms(100), note("end"), new KeyValue(x, 100.0)));
        t.setCycleCount(Animation.INDEFINITE);
        t.setOnFinished(note("finished"));
        assertEquals(Duration.INDEFINITE, t.getTotalDuration());
        t.play();
        FrameClock.advance(100050);
        assertSame(Animation.Status.RUNNING, t.getStatus());
        assertEquals(50.0, x.get(), EXACT);
        assertEquals(1000, log.size());
        assertFalse(log.contains("finished"));
        t.stop();
        assertFalse(FrameClock.isActive());
        try {
            t.setCycleCount(0);
            fail("no cycles is not a cycle count");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void aZeroDurationTimelineFinishesOnTheNextPulse() {
        DoubleProperty x = new SimpleDoubleProperty(1);
        Timeline t = new Timeline(new KeyFrame(Duration.ZERO, note("frame"), new KeyValue(x, 9.0)));
        t.setOnFinished(note("finished"));
        t.play();
        assertSame(Animation.Status.RUNNING, t.getStatus());
        assertTrue(log.isEmpty());
        assertEquals(1.0, x.get(), EXACT);
        FrameClock.advance(0);
        assertEquals(9.0, x.get(), EXACT);
        assertEquals(Arrays.asList("frame", "finished"), log);
        assertSame(Animation.Status.STOPPED, t.getStatus());

        log.clear();
        Timeline empty = new Timeline();
        empty.setOnFinished(note("finished"));
        empty.play();
        FrameClock.advance(16);
        assertEquals(Arrays.asList("finished"), log);
    }

    @Test
    public void theDelayComesFirst() {
        DoubleProperty x = new SimpleDoubleProperty(0);
        Timeline t = line(x, 100, 1000);
        t.setDelay(ms(200));
        t.play();
        FrameClock.advance(150);
        assertEquals(0.0, x.get(), EXACT);
        FrameClock.advance(150);
        assertEquals(10.0, x.get(), EXACT);
    }

    @Test
    public void aHandlerMayStopTheAnimation() {
        DoubleProperty x = new SimpleDoubleProperty(0);
        final Timeline t = new Timeline();
        t.getKeyFrames().addAll(new KeyFrame(ms(100), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                log.add("stopping");
                t.stop();
            }
        }), new KeyFrame(ms(200), note("late")), new KeyFrame(ms(300), new KeyValue(x, 300.0)));
        t.setOnFinished(note("finished"));
        t.play();
        FrameClock.advance(1000);
        assertEquals(Arrays.asList("stopping"), log);
        assertSame(Animation.Status.STOPPED, t.getStatus());
        assertEquals(100.0, x.get(), EXACT);
        assertFalse(FrameClock.isActive());
    }

    // ---- composition ----

    @Test
    public void sequentialPlaysOneAfterTheOther() {
        DoubleProperty x = new SimpleDoubleProperty(0);
        DoubleProperty y = new SimpleDoubleProperty(0);
        Timeline a = line(x, 100, 100);
        a.setOnFinished(note("a"));
        PauseTransition pause = new PauseTransition(ms(50));
        pause.setOnFinished(note("pause"));
        Timeline b = line(y, 10, 100);
        b.setOnFinished(note("b"));
        SequentialTransition seq = new SequentialTransition(a, pause, b);
        seq.setOnFinished(note("seq"));
        assertEquals(ms(250), seq.getCycleDuration());
        seq.play();
        FrameClock.advance(50);
        assertEquals(50.0, x.get(), EXACT);
        assertEquals(0.0, y.get(), EXACT);
        assertTrue(log.isEmpty());
        FrameClock.advance(75);
        assertEquals(100.0, x.get(), EXACT);
        assertEquals(0.0, y.get(), EXACT);
        assertEquals(Arrays.asList("a"), log);
        FrameClock.advance(75);
        assertEquals(5.0, y.get(), EXACT);
        assertEquals(Arrays.asList("a", "pause"), log);
        FrameClock.advance(50);
        assertEquals(10.0, y.get(), EXACT);
        assertEquals(Arrays.asList("a", "pause", "b", "seq"), log);
        assertSame(Animation.Status.STOPPED, seq.getStatus());

        // One big frame runs it all, in order.
        log.clear();
        x.set(0);
        y.set(0);
        seq.play();
        FrameClock.advance(10000);
        assertEquals(Arrays.asList("a", "pause", "b", "seq"), log);
        assertEquals(100.0, x.get(), EXACT);
        assertEquals(10.0, y.get(), EXACT);
    }

    @Test
    public void aLaterChildStartsFromWhatTheEarlierOnesLeft() {
        DoubleProperty x = new SimpleDoubleProperty(0);
        SequentialTransition seq = new SequentialTransition(line(x, 100, 100), line(x, 0, 100));
        seq.play();
        FrameClock.advance(150);
        assertEquals(50.0, x.get(), EXACT);
        FrameClock.advance(50);
        assertEquals(0.0, x.get(), EXACT);
    }

    @Test
    public void aSequenceRunsBackwardsJumpsAndPauses() {
        DoubleProperty x = new SimpleDoubleProperty(0);
        DoubleProperty y = new SimpleDoubleProperty(0);
        SequentialTransition seq = new SequentialTransition(line(x, 100, 100), line(y, 10, 100));
        seq.setCycleCount(2);
        seq.setAutoReverse(true);
        seq.setOnFinished(note("seq"));
        seq.play();
        FrameClock.advance(150);
        assertEquals(100.0, x.get(), EXACT);
        assertEquals(5.0, y.get(), EXACT);
        // 250 ms: 50 ms into the way back, which undoes the second child first.
        FrameClock.advance(100);
        assertEquals(100.0, x.get(), EXACT);
        assertEquals(5.0, y.get(), EXACT);
        assertEquals(-1.0, seq.getCurrentRate(), EXACT);
        seq.pause();
        FrameClock.advance(1000);
        assertEquals(5.0, y.get(), EXACT);
        seq.play();
        FrameClock.advance(100);
        assertEquals(50.0, x.get(), EXACT);
        assertEquals(0.0, y.get(), EXACT);
        assertTrue(log.isEmpty());
        FrameClock.advance(50);
        assertEquals(0.0, x.get(), EXACT);
        assertEquals(Arrays.asList("seq"), log);

        // A jump shows every child as it is at that time.
        seq.setAutoReverse(false);
        seq.setCycleCount(1);
        seq.play();
        FrameClock.advance(20);
        seq.jumpTo(ms(150));
        assertEquals(100.0, x.get(), EXACT);
        assertEquals(5.0, y.get(), EXACT);
        seq.jumpTo(ms(50));
        assertEquals(50.0, x.get(), EXACT);
        assertEquals(0.0, y.get(), EXACT);
        FrameClock.advance(100);
        assertEquals(100.0, x.get(), EXACT);
        assertEquals(5.0, y.get(), EXACT);
        seq.stop();
        assertEquals(Duration.ZERO, seq.getCurrentTime());
    }

    @Test
    public void parallelPlaysTogetherAndLastsAsLongAsTheLongest() {
        DoubleProperty x = new SimpleDoubleProperty(0);
        DoubleProperty y = new SimpleDoubleProperty(0);
        Timeline a = line(x, 100, 100);
        a.setOnFinished(note("a"));
        Timeline b = line(y, 10, 200);
        b.setOnFinished(note("b"));
        ParallelTransition par = new ParallelTransition(a, b);
        par.setOnFinished(note("par"));
        assertEquals(ms(200), par.getCycleDuration());
        par.play();
        FrameClock.advance(50);
        assertEquals(50.0, x.get(), EXACT);
        assertEquals(2.5, y.get(), EXACT);
        FrameClock.advance(100);
        assertEquals(100.0, x.get(), EXACT);
        assertEquals(7.5, y.get(), EXACT);
        assertEquals(Arrays.asList("a"), log);
        FrameClock.advance(50);
        assertEquals(10.0, y.get(), EXACT);
        assertEquals(Arrays.asList("a", "b", "par"), log);
    }

    @Test
    public void nestedCompositionAndCycles() {
        DoubleProperty x = new SimpleDoubleProperty(0);
        DoubleProperty y = new SimpleDoubleProperty(0);
        DoubleProperty z = new SimpleDoubleProperty(0);
        Timeline a = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(x, 0.0)),
                new KeyFrame(ms(100), new KeyValue(x, 100.0)));
        Timeline b = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(y, 0.0)),
                new KeyFrame(ms(200), new KeyValue(y, 10.0)));
        Timeline c = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(z, 0.0)),
                new KeyFrame(ms(100), new KeyValue(z, 1.0)));
        b.setOnFinished(note("b"));
        c.setOnFinished(note("c"));
        ParallelTransition par = new ParallelTransition(a, b);
        par.setOnFinished(note("par"));
        SequentialTransition seq = new SequentialTransition(par, c);
        seq.setCycleCount(2);
        seq.setOnFinished(note("seq"));
        assertEquals(ms(300), seq.getCycleDuration());
        assertEquals(ms(600), seq.getTotalDuration());
        seq.play();
        FrameClock.advance(250);
        assertEquals(100.0, x.get(), EXACT);
        assertEquals(10.0, y.get(), EXACT);
        assertEquals(0.5, z.get(), EXACT);
        assertEquals(Arrays.asList("b", "par"), log);
        // 400 ms: 100 ms into the second cycle.
        FrameClock.advance(150);
        assertEquals(100.0, x.get(), EXACT);
        assertEquals(5.0, y.get(), EXACT);
        assertEquals(Arrays.asList("b", "par", "c"), log);
        FrameClock.advance(200);
        assertEquals(Arrays.asList("b", "par", "c", "b", "par", "c", "seq"), log);
        assertEquals(1.0, z.get(), EXACT);
    }

    @Test
    public void aChildCannotBePlayedOnItsOwn() {
        DoubleProperty x = new SimpleDoubleProperty(0);
        Timeline child = line(x, 100, 100);
        SequentialTransition seq = new SequentialTransition(child);
        try {
            child.play();
            fail("a child is played by its parent");
        } catch (IllegalStateException expected) {
            // expected
        }
        try {
            child.pause();
            fail();
        } catch (IllegalStateException expected) {
            // expected
        }
        try {
            child.stop();
            fail();
        } catch (IllegalStateException expected) {
            // expected
        }
        try {
            child.jumpTo(ms(10));
            fail();
        } catch (IllegalStateException expected) {
            // expected
        }
        try {
            new ParallelTransition(child);
            fail("an animation has one parent");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        // Taken out, it is its own again.
        seq.getChildren().clear();
        assertEquals(Duration.ZERO, seq.getCycleDuration());
        child.play();
        FrameClock.advance(50);
        assertEquals(50.0, x.get(), EXACT);
    }

    // ---- transitions ----

    private static Region node() {
        Region r = new Region();
        r.resize(10, 10);
        return r;
    }

    @Test
    public void fadeChangesTheOpacity() {
        Region n = node();
        FadeTransition fade = new FadeTransition(ms(1000), n);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setInterpolator(Interpolator.LINEAR);
        assertEquals(ms(1000), fade.getCycleDuration());
        fade.play();
        FrameClock.advance(250);
        assertEquals(0.75, n.getOpacity(), EXACT);
        FrameClock.advance(750);
        assertEquals(0.0, n.getOpacity(), EXACT);

        // From what the node has, by an amount, eased.
        n.setOpacity(0.2);
        FadeTransition by = new FadeTransition(ms(1000), n);
        by.setByValue(0.4);
        assertSame(Interpolator.EASE_BOTH, by.getInterpolator());
        by.play();
        FrameClock.advance(100);
        assertEquals(0.2 + 0.4 * 0.03125, n.getOpacity(), EXACT);
        FrameClock.advance(400);
        assertEquals(0.4, n.getOpacity(), EXACT);
        FrameClock.advance(500);
        assertEquals(0.6, n.getOpacity(), 1e-12);
    }

    @Test
    public void translateMovesTheNodeThroughItsRealProperties() {
        Region n = node();
        final int[] invalidations = {0};
        n.boundsInParentProperty().addListener(new javafx.beans.InvalidationListener() {
            @Override
            public void invalidated(javafx.beans.Observable observable) {
                invalidations[0]++;
            }
        });
        n.getBoundsInParent();
        TranslateTransition move = new TranslateTransition(ms(1000), n);
        move.setByX(100);
        move.setFromY(10);
        move.setToY(50);
        move.setInterpolator(Interpolator.LINEAR);
        move.play();
        FrameClock.advance(250);
        assertEquals(25.0, n.getTranslateX(), EXACT);
        assertEquals(20.0, n.getTranslateY(), EXACT);
        assertTrue("the node's geometry was invalidated", invalidations[0] > 0);
        assertEquals(25.0, n.getBoundsInParent().getMinX(), EXACT);
        assertEquals(20.0, n.getBoundsInParent().getMinY(), EXACT);
        FrameClock.advance(750);
        assertEquals(100.0, n.getTranslateX(), EXACT);
        assertEquals(50.0, n.getTranslateY(), EXACT);
    }

    @Test
    public void scaleAndRotate() {
        Region n = node();
        ScaleTransition scale = new ScaleTransition(ms(1000), n);
        scale.setToX(2);
        scale.setByY(2);
        scale.setInterpolator(Interpolator.LINEAR);
        RotateTransition rotate = new RotateTransition(ms(1000), n);
        rotate.setByAngle(360);
        rotate.setInterpolator(Interpolator.LINEAR);
        scale.play();
        rotate.play();
        FrameClock.advance(500);
        assertEquals(1.5, n.getScaleX(), EXACT);
        assertEquals(2.0, n.getScaleY(), EXACT);
        assertEquals(180.0, n.getRotate(), EXACT);
        FrameClock.advance(500);
        assertEquals(2.0, n.getScaleX(), EXACT);
        assertEquals(3.0, n.getScaleY(), EXACT);
        assertEquals(360.0, n.getRotate(), EXACT);

        RotateTransition back = new RotateTransition(ms(100), n);
        back.setFromAngle(90);
        back.setToAngle(0);
        back.setInterpolator(Interpolator.LINEAR);
        back.play();
        FrameClock.advance(50);
        assertEquals(45.0, n.getRotate(), EXACT);
    }

    @Test
    public void childrenAnimateTheNodeOfTheirParent() {
        Region n = node();
        FadeTransition fade = new FadeTransition(ms(100));
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setInterpolator(Interpolator.LINEAR);
        TranslateTransition move = new TranslateTransition(ms(100));
        move.setToX(40);
        move.setInterpolator(Interpolator.LINEAR);
        ParallelTransition par = new ParallelTransition(n, fade, move);
        SequentialTransition seq = new SequentialTransition(new PauseTransition(ms(100)), par);
        assertSame(n, par.getNode());
        seq.play();
        FrameClock.advance(100);
        assertEquals(1.0, n.getOpacity(), EXACT);
        FrameClock.advance(50);
        assertEquals(0.5, n.getOpacity(), EXACT);
        assertEquals(20.0, n.getTranslateX(), EXACT);
        FrameClock.advance(50);
        assertEquals(0.0, n.getOpacity(), EXACT);
        assertEquals(40.0, n.getTranslateX(), EXACT);
        assertSame(Animation.Status.STOPPED, seq.getStatus());
    }

    @Test
    public void aTransitionOfOnesOwn() {
        final List<Double> fractions = new ArrayList<Double>();
        Transition t = new Transition() {
            {
                setCycleDuration(ms(200));
                setInterpolator(Interpolator.LINEAR);
            }

            @Override
            protected void interpolate(double frac) {
                fractions.add(Double.valueOf(frac));
            }
        };
        t.setOnFinished(note("finished"));
        t.play();
        FrameClock.advance(50);
        FrameClock.advance(100);
        FrameClock.advance(100);
        assertEquals(Arrays.asList(Double.valueOf(0.25), Double.valueOf(0.75), Double.valueOf(1.0)), fractions);
        assertEquals(Arrays.asList("finished"), log);

        log.clear();
        PauseTransition pause = new PauseTransition(ms(300));
        pause.setOnFinished(note("pause over"));
        pause.play();
        FrameClock.advance(299);
        assertTrue(log.isEmpty());
        FrameClock.advance(1);
        assertEquals(Arrays.asList("pause over"), log);
    }

    // ---- the timer ----

    @Test
    public void animationTimerGetsEveryFrameUntilItIsStopped() {
        final List<Long> times = new ArrayList<Long>();
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                times.add(Long.valueOf(now));
            }
        };
        FrameClock.advance(16);
        assertTrue("not started yet", times.isEmpty());
        timer.start();
        timer.start();
        assertTrue(FrameClock.isActive());
        FrameClock.advance(16);
        FrameClock.advance(16);
        FrameClock.advance(34);
        assertEquals(3, times.size());
        assertEquals(16000000L, times.get(1).longValue() - times.get(0).longValue());
        assertEquals(34000000L, times.get(2).longValue() - times.get(1).longValue());
        timer.stop();
        assertFalse(FrameClock.isActive());
        FrameClock.advance(16);
        assertEquals(3, times.size());
        timer.start();
        FrameClock.advance(16);
        assertEquals(4, times.size());
        assertTrue(times.get(3).longValue() > times.get(2).longValue());
        timer.stop();
    }

    @Test
    public void aTimerMayStopItselfInsideAFrame() {
        final int[] calls = {0};
        final AnimationTimer[] self = new AnimationTimer[1];
        self[0] = new AnimationTimer() {
            @Override
            public void handle(long now) {
                calls[0]++;
                self[0].stop();
            }
        };
        self[0].start();
        FrameClock.advance(16);
        FrameClock.advance(16);
        assertEquals(1, calls[0]);
        try {
            FrameClock.advance(-1);
            fail("time does not run backwards");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
