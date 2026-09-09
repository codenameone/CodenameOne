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
        stop(false);
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
    }

    public void upperBound(double v) {
        this.upperBound = v;
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

    public void forward(Double from) {
        if (from != null) {
            currentValue = clamp(from);
        }
        repeating = false;
        beginRun(upperBound, durationMs, AnimationStatus.forward);
    }

    /** No-argument {@code forward()} — usable as a bare {@code VoidCallback} tear-off. */
    public void forward() {
        forward(null);
    }

    /** No-argument {@code reverse()} — usable as a bare {@code VoidCallback} tear-off. */
    public void reverse() {
        reverse(null);
    }

    public void reverse(Double from) {
        if (from != null) {
            currentValue = clamp(from);
        }
        repeating = false;
        long d = reverseDurationMs >= 0 ? reverseDurationMs : durationMs;
        beginRun(lowerBound, d, AnimationStatus.reverse);
    }

    /**
     * Drives the controller with a fling toward the bound implied by the sign
     * of {@code velocity} — Flutter's {@code AnimationController.fling}. A
     * positive velocity flings toward {@code upperBound}, a negative one toward
     * {@code lowerBound}. The {@code springDescription} (the spring modeling the
     * fling's settle) and {@code animationBehavior} are captured for API shape;
     * this runtime plays a plain timed run to the target bound.
     */
    public void fling(double velocity, Object springDescription, AnimationBehavior animationBehavior) {
        repeating = false;
        if (velocity < 0.0) {
            beginRun(lowerBound, durationMs, AnimationStatus.reverse);
        } else {
            beginRun(upperBound, durationMs, AnimationStatus.forward);
        }
    }

    public void animateTo(double target, Duration duration, Curve curve) {
        repeating = false;
        long d = duration != null ? duration.inMilliseconds() : durationMs;
        AnimationStatus dir = target >= currentValue ? AnimationStatus.forward : AnimationStatus.reverse;
        beginRun(clamp(target), d, dir);
    }

    public void animateBack(double target, Duration duration, Curve curve) {
        repeating = false;
        long d = duration != null ? duration.inMilliseconds()
                : (reverseDurationMs >= 0 ? reverseDurationMs : durationMs);
        beginRun(clamp(target), d, AnimationStatus.reverse);
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
        running = false;
        generation++;
        // Leave the frame clock immediately; it stops itself once nothing is running.
        FrameDriver.remove(this);
    }

    public void reset() {
        stop(false);
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
        stop(false);
    }

    // ------------------------------------------------------------------
    // Driving
    // ------------------------------------------------------------------

    private void beginRun(double target, long dMs, AnimationStatus phase) {
        generation++;
        final int gen = generation;
        running = true;
        runStartValue = currentValue;
        runTargetValue = target;
        runDurationMs = Math.max(0, dMs);
        runStatus = phase;
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

        if (runDurationMs == 0 || runStartValue == runTargetValue || !Display.isInitialized()) {
            finishRun(gen);
            return;
        }
        scheduleTick(gen);
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
            // First tick of this run: it defines t = 0. The value is already runStartValue,
            // so there is nothing to notify - fall through and let the next tick move it.
            runStartTime = now();
            FrameDriver.noteAdvance(0);
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
        return System.currentTimeMillis();
    }
}
