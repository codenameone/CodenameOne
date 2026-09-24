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
package com.codename1.flutter.animation;

import com.codename1.ui.CN;
import com.codename1.ui.Display;

import dart.core.Duration;

/**
 * Drives an animation value between {@code lowerBound} and {@code upperBound}
 * over a {@link Duration} — Flutter's {@code AnimationController}. The
 * controller joins the shared {@link FrameDriver} clock, which rides Codename
 * One's own animation loop, and advances once per frame — firing value
 * listeners each frame and status listeners at the transitions. The
 * {@code vsync} TickerProvider is accepted for API shape but not otherwise
 * used, because the frame clock already is the vsync.
 *
 * <p>A run is timed from its FIRST tick, not from the call that started it,
 * which is what Flutter's {@code Ticker} does; see {@code beginRun}.</p>
 *
 * <p>When CN1's Display is not initialized (headless), animations complete
 * synchronously so logic that awaits {@code forward()} still progresses.</p>
 */
public class AnimationController extends Animation<Double> {

    private double lowerBound = 0.0;
    private double upperBound = 1.0;
    private double currentValue;
    private long durationMs = 300;
    private long reverseDurationMs = -1;
    private AnimationStatus status = AnimationStatus.dismissed;
    private AnimationBehavior animationBehavior = AnimationBehavior.normal;

    // Active run state.
    private boolean running;
    private int generation;
    /** {@link #runStartTime} before the first tick has established the run's zero point. */
    private static final long UNSTARTED = Long.MIN_VALUE;
    private long runStartTime = UNSTARTED;
    private long runDurationMs;
    private double runStartValue;
    private double runTargetValue;
    private AnimationStatus runStatus;
    /**
     * The curve this run eases along, or null for a linear run.
     *
     * <p>{@code animateTo}/{@code animateBack} take a curve in Flutter and it shapes the
     * controller's own progress; {@code forward}, {@code reverse} and {@code fling} are
     * linear. Dropping it made every eased call linear, which is not a subtle difference:
     * Reply opens its drawer with {@code animateTo(0.4, curve: Easing.legacy)}, and a
     * linear run starts and stops abruptly where the real one eases in and settles.</p>
     */
    private Curve runCurve;

    // Repeat config.
    private boolean repeating;
    private boolean repeatReverse;
    private double repeatMin;
    private double repeatMax;

    public AnimationController() {
    }

    // ------------------------------------------------------------------
    // Named-parameter setters (constructor arguments)
    // ------------------------------------------------------------------

    public void duration(Duration v) {
        if (v != null) {
            this.durationMs = v.inMilliseconds();
        }
    }

    public void reverseDuration(Duration v) {
        if (v != null) {
            this.reverseDurationMs = v.inMilliseconds();
        }
    }

    /**
     * The Dart {@code value} setter — both the constructor {@code value:}
     * argument and the imperative {@code controller.value = v} assignment map
     * here (the transpiler emits the overloaded {@code value(v)} method). Stops
     * any running animation, clamps to the bounds, and notifies listeners.
     */
    public void value(double v) {
        // Canceled, as Flutter's value setter, reset and dispose are: the interrupted run's
        // future stays pending rather than resolving as if its target had been reached.
        stop(Boolean.TRUE);
        requestedValue = v;
        double clamped = clamp(v);
        this.currentValue = clamped;
        AnimationStatus newStatus = statusForValue(clamped);
        boolean statusChanged = newStatus != status;
        this.status = newStatus;
        notifyListeners();
        if (statusChanged) {
            notifyStatusListeners(status);
        }
    }

    public void lowerBound(double v) {
        this.lowerBound = v;
        rebaseOnBounds();
    }

    public void upperBound(double v) {
        this.upperBound = v;
        rebaseOnBounds();
    }

    /// The value last asked for through value(), or null if none was.
    private Double requestedValue;

    /// Re-derives the value after a bound changes -- which in Flutter only happens
    /// at construction, where the bounds are final.
    ///
    /// Flutter starts a controller at its lower bound unless `value:` is given.
    /// The no-argument Java controller starts at 0 and the bound setters only
    /// stored the bound, so AnimationController(lowerBound: 10, upperBound: 20)
    /// reported and animated from 0 -- outside its own range -- with the wrong
    /// status. The transpiler also applies named arguments as setters in source
    /// order, so `value: 15` given before the bounds had already been clamped
    /// against the default 0..1 and stored as 1. Keeping the requested value and
    /// re-clamping it here makes the result independent of that order.
    private void rebaseOnBounds() {
        currentValue = requestedValue != null ? clamp(requestedValue) : lowerBound;
        status = statusForValue(currentValue);
    }

    public void vsync(TickerProvider v) {
        // self-driven; provider unused
    }

    public void debugLabel(String v) {
        // ignored
    }

    /**
     * How the controller behaves when animation is disabled by the platform —
     * Flutter's {@code AnimationController.animationBehavior}. Captured for API
     * shape; this runtime always animates (it does not consult a
     * reduce-motion setting), matching {@link AnimationBehavior#preserve}.
     */
    public void animationBehavior(AnimationBehavior v) {
        this.animationBehavior = v;
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    @Override
    public Double value() {
        return currentValue;
    }

    @Override
    public AnimationStatus status() {
        return status;
    }

    public Duration duration() {
        return Duration.ofMicroseconds(durationMs * 1000);
    }

    /** Flutter's {@code controller.view}: the controller is its own view. */
    public Animation<Double> view() {
        return this;
    }

    // ------------------------------------------------------------------
    // Playback controls
    // ------------------------------------------------------------------

    public dart.async.Future<Object> forward(Double from) {
        if (from != null) {
            currentValue = clamp(from);
        }
        repeating = false;
        dart.async.Future<Object> done = newRun();
        beginRun(upperBound, durationMs, AnimationStatus.forward);
        return done;
    }

    /** No-argument {@code forward()} — usable as a bare {@code VoidCallback} tear-off. */
    public dart.async.Future<Object> forward() {
        return forward(null);
    }

    /** No-argument {@code reverse()} — usable as a bare {@code VoidCallback} tear-off. */
    public dart.async.Future<Object> reverse() {
        return reverse(null);
    }

    public dart.async.Future<Object> reverse(Double from) {
        if (from != null) {
            currentValue = clamp(from);
        }
        repeating = false;
        long d = reverseDurationMs >= 0 ? reverseDurationMs : durationMs;
        dart.async.Future<Object> done = newRun();
        beginRun(lowerBound, d, AnimationStatus.reverse);
        return done;
    }

    /**
     * Drives the controller with a fling toward the bound implied by the sign
     * of {@code velocity} — Flutter's {@code AnimationController.fling}. A
     * positive velocity flings toward {@code upperBound}, a negative one toward
     * {@code lowerBound}. The {@code springDescription} (the spring modeling the
     * fling's settle) and {@code animationBehavior} are captured for API shape;
     * this runtime plays a plain timed run to the target bound.
     */
    public dart.async.Future<Object> fling(double velocity, Object springDescription,
                                          AnimationBehavior animationBehavior) {
        repeating = false;
        dart.async.Future<Object> done = newRun();
        if (velocity < 0.0) {
            beginRun(lowerBound, durationMs, AnimationStatus.reverse);
        } else {
            beginRun(upperBound, durationMs, AnimationStatus.forward);
        }
        return done;
    }

    public dart.async.Future<Object> animateTo(double target, Duration duration, Curve curve) {
        repeating = false;
        AnimationStatus dir = target >= currentValue
                ? AnimationStatus.forward : AnimationStatus.reverse;
        dart.async.Future<Object> done = newRun();
        beginRun(clamp(target), simulationMillis(target, duration, dir), dir, curve);
        return done;
    }

    /**
     * How long an {@code animateTo}/{@code animateBack} run lasts.
     *
     * <p>A duration given explicitly is that duration. Without one, the controller's
     * duration describes crossing the WHOLE range, and the run gets the fraction of it
     * this move actually covers -- Flutter's {@code _animateToInternal}:
     * {@code directionDuration * remainingFraction}.</p>
     *
     * <p>Not a detail. Reply opens its mailbox drawer with {@code animateTo(0.4)} on a
     * 300ms controller, which is a 120ms animation; running the full 300 made the drawer
     * take two and a half times as long to arrive as the reference, and the same error
     * slowed the drop arrow beside it and the search page behind it. Measured against the
     * reference frame by frame, ours had barely moved at the point the reference was
     * finished.</p>
     */
    /// Seam for AnimateToDurationTest: the arithmetic, without needing a frame clock.
    long simulationMillisForTest(double target, Duration explicit, AnimationStatus dir) {
        return simulationMillis(target, explicit, dir);
    }

    private long simulationMillis(double target, Duration explicit, AnimationStatus dir) {
        if (explicit != null) {
            // Flutter does not animate at all when asked to go where it already is.
            return target == currentValue ? 0 : explicit.inMilliseconds();
        }
        double range = upperBound - lowerBound;
        double remaining = range > 0 ? Math.abs(target - currentValue) / range : 1.0;
        long base = dir == AnimationStatus.reverse && reverseDurationMs >= 0
                ? reverseDurationMs : durationMs;
        return Math.round(base * remaining);
    }

    public dart.async.Future<Object> animateBack(double target, Duration duration, Curve curve) {
        repeating = false;
        dart.async.Future<Object> done = newRun();
        beginRun(clamp(target),
                simulationMillis(target, duration, AnimationStatus.reverse),
                AnimationStatus.reverse, curve);
        return done;
    }

    /// The future the CURRENT run completes, as Flutter's TickerFuture: it
    /// completes when this run reaches its target, and never when the run is
    /// stopped or replaced by another -- Flutter's plain TickerFuture does not
    /// complete on cancellation either.
    ///
    /// These controls were void, so `await controller.forward()` returned at once
    /// and `.then(...)` ran before the first frame: "reverse, then remove the
    /// widget" removed it while the reverse was still to play. Created BEFORE the
    /// run begins, because a zero-length run finishes inside beginRun.
    private dart.async.Completer<Object> runCompleter;

    private dart.async.Future<Object> newRun() {
        dart.async.Completer<Object> c = new dart.async.Completer<Object>();
        runCompleter = c;
        return c.future();
    }

    public void repeat(Double min, Double max, Boolean reverse, Duration period) {
        repeating = true;
        repeatReverse = reverse != null && reverse;
        repeatMin = min != null ? min : lowerBound;
        repeatMax = max != null ? max : upperBound;
        long d = period != null ? period.inMilliseconds() : durationMs;
        currentValue = repeatMin;
        beginRun(repeatMax, d, AnimationStatus.forward);
    }

    public void stop(Boolean canceled) {
        // A canceled run's future never completes, as in Flutter -- but
        // stop(canceled: false) completes it, and ignoring the flag left every
        // `await controller.forward()` pending for good.
        dart.async.Completer<Object> c = runCompleter;
        runCompleter = null;
        settleStoppedRun(c, canceled);
        running = false;
        generation++;
        // Leave the frame clock immediately; it stops itself once nothing is running.
        FrameDriver.remove(this);
    }

    /**
     * What stop does with the stopped run's future: completes it for
     * {@code canceled: false}, and leaves it pending otherwise -- Flutter's default is
     * canceled, and a canceled TickerFuture never completes.
     */
    static void settleStoppedRun(dart.async.Completer<Object> run, Boolean canceled) {
        if (run != null && canceled != null && !canceled.booleanValue()) {
            run.complete(null);
        }
    }

    public void reset() {
        // Canceled, as Flutter's value setter, reset and dispose are: the interrupted run's
        // future stays pending rather than resolving as if its target had been reached.
        stop(Boolean.TRUE);
        currentValue = lowerBound;
        AnimationStatus newStatus = statusForValue(currentValue);
        boolean changed = newStatus != status;
        status = newStatus;
        notifyListeners();
        if (changed) {
            notifyStatusListeners(status);
        }
    }

    public void dispose() {
        // Canceled, as Flutter's value setter, reset and dispose are: the interrupted run's
        // future stays pending rather than resolving as if its target had been reached.
        stop(Boolean.TRUE);
    }

    // ------------------------------------------------------------------
    // Driving
    // ------------------------------------------------------------------

    private void beginRun(double target, long dMs, AnimationStatus phase) {
        beginRun(target, dMs, phase, null);
    }

    private void beginRun(double target, long dMs, AnimationStatus phase, Curve curve) {
        generation++;
        final int gen = generation;
        running = true;
        runStartValue = currentValue;
        runTargetValue = target;
        runDurationMs = Math.max(0, dMs);
        runStatus = phase;
        runCurve = curve;
        // NOT now(): the run is timed from its FIRST tick, which is what Flutter's Ticker
        // does (it records _startTime inside the first frame callback). The gap between
        // "start the animation" and "the clock reaches it" is setup - the setState that
        // starts it, the build it triggers, the paint of that build - and charging it to
        // the curve is what makes a short animation snap. Measured on the gallery's
        // category expand that gap was 223ms against a 200ms duration, so the first tick
        // already had t >= 1 and the animation only ever showed its end state.
        runStartTime = UNSTARTED;

        if (status != phase) {
            status = phase;
            notifyStatusListeners(status);
        }

        if (runDurationMs == 0 || runStartValue == runTargetValue) {
            finishRun(gen);
            return;
        }
        if (!Display.isInitialized()) {
            // Headless: there is no frame clock to advance us, so the run collapses to its
            // end state. It still gets its first tick, so a listener that reacts to the
            // start of a run behaves the same way here as on a device -- which is the kind
            // of difference a headless test exists to rule out.
            firstTick();
            finishRun(gen);
            return;
        }
        scheduleTick(gen);
    }

    /**
     * The opening frame of a run. It defines t = 0, so the value is still
     * {@code runStartValue} and nothing moves -- but the listeners are notified anyway.
     *
     * <p>Flutter's Ticker calls its callback on the first frame with an elapsed of zero
     * and {@code AnimationController._tick} notifies unconditionally, so a listener is
     * guaranteed one call while the run is still at the value it started from. Apps build
     * on that: Reply's bottom drawer animates a controller up from 0, and the rebuild that
     * makes the drawer visible comes from a listener that only calls setState while the
     * value is below 0.01. Suppressing this notification -- on the reasonable-sounding
     * grounds that the value has not moved yet -- meant the first call a listener ever saw
     * was already past that threshold. The drawer's state flipped and its arrow turned,
     * and no panel was ever built.</p>
     */
    private void firstTick() {
        FrameDriver.noteAdvance(0);
        notifyListeners();
    }

    private void scheduleTick(final int gen) {
        // Join the shared frame clock rather than chaining a timer of our own: N
        // animations then cost one wakeup and one build flush per frame between them.
        FrameDriver.add(this);
    }

    /**
     * How far through a run of {@code durationMs} an elapsed time is, under the
     * scheduler's current time dilation.
     *
     * <p>{@code scheduler.timeDilation} stretches every animation in the app. It was
     * declared here and never read, so the gallery's own "Slow motion" switch -- a
     * control whose entire purpose is to let a motion design be inspected -- moved
     * nothing at all. Flutter divides the frame timestamp by it; dividing the run's
     * duration is the same thing and keeps the run's zero point where the first tick
     * put it.</p>
     *
     * <p>A dilation that is zero, negative or NaN would divide the animation by zero or
     * run it backwards. Flutter asserts on it; here it means real time, so a bad value
     * cannot wedge the UI.</p>
     *
     * <p>Package-private and static so it can be asserted directly -- the controller's
     * clock is the system clock, and a test that reimplements this arithmetic to check
     * it would be testing its own copy.</p>
     */
    static double progress(long elapsedMs, long durationMs) {
        double dilation = com.codename1.flutter.scheduler.SchedulerLib.timeDilation;
        if (!(dilation > 0)) {
            dilation = 1.0;
        }
        double effective = durationMs * dilation;
        return effective <= 0 ? 1.0 : elapsedMs / effective;
    }

    /**
     * Advances this animation to the current time. Called once per frame by
     * {@link FrameDriver}; finishing removes it from the clock.
     */
    void advance() {
        if (!running) {
            FrameDriver.remove(this);
            return;
        }
        int gen = generation;
        if (runStartTime == UNSTARTED) {
            runStartTime = now();
            firstTick();
            return;
        }
        long elapsed = now() - runStartTime;
        FrameDriver.noteAdvance(elapsed);
        double t = progress(elapsed, runDurationMs);
        if (t >= 1.0) {
            finishRun(gen);
            if (!running) {
                FrameDriver.remove(this);
            }
            return;
        }
        if (runCurve != null) {
            t = runCurve.transform(t);
        }
        currentValue = runStartValue + (runTargetValue - runStartValue) * t;
        notifyListeners();
    }

    private void finishRun(int gen) {
        currentValue = runTargetValue;
        running = false;
        notifyListeners();
        AnimationStatus terminal = statusForValue(currentValue);
        if (terminal != status) {
            status = terminal;
            notifyStatusListeners(status);
        }
        // Completed after the listeners have seen the final value, so code that
        // awaited the run observes the finished state. A repeating run never
        // completes -- it has no end -- which is also Flutter's behaviour.
        if (!repeating && runCompleter != null) {
            dart.async.Completer<Object> c = runCompleter;
            runCompleter = null;
            c.complete(null);
        }
        if (repeating && Display.isInitialized()) {
            if (repeatReverse) {
                double nextTarget = currentValue >= repeatMax ? repeatMin : repeatMax;
                AnimationStatus phase = nextTarget >= currentValue
                        ? AnimationStatus.forward : AnimationStatus.reverse;
                beginRun(nextTarget, runDurationMs, phase);
            } else {
                currentValue = repeatMin;
                beginRun(repeatMax, runDurationMs, AnimationStatus.forward);
            }
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private AnimationStatus statusForValue(double v) {
        if (v <= lowerBound) {
            return AnimationStatus.dismissed;
        }
        if (v >= upperBound) {
            return AnimationStatus.completed;
        }
        return status == AnimationStatus.reverse ? AnimationStatus.reverse : AnimationStatus.forward;
    }

    private double clamp(double v) {
        if (v < lowerBound) {
            return lowerBound;
        }
        if (v > upperBound) {
            return upperBound;
        }
        return v;
    }

    private static long now() {
        // Not System.currentTimeMillis() directly: a harness comparing this runtime's
        // motion against another stack's has to be able to ask both for the same
        // animation time. See MotionClock -- released, which is every application, this
        // IS the wall clock.
        return MotionClock.now();
    }
}
