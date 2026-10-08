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
package javafx.animation;

import com.codename1.fxcompat.runtime.FrameClock;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

/// Something that plays over time: the playhead, its speed and
/// direction, its cycles, and the controls that start, pause and stop
/// it. [Timeline] and the transitions say what playing does.
///
/// An animation runs on the JavaFX application thread, one step per
/// frame. A cycle lasts [#getCycleDuration()] and is played
/// [#getCycleCount()] times; with [#isAutoReverse()] every second cycle
/// runs backwards. A negative rate plays towards the start, and the
/// animation then finishes when it gets there.
///
/// The cycle duration, the cycle count, auto reverse and the delay are
/// read when the animation starts; changing them while it runs takes
/// effect the next time it is started. The rate is read on every frame.
///
/// An animation that is a child of a [SequentialTransition] or a
/// [ParallelTransition] is played by its parent, and its own controls
/// throw `IllegalStateException`.
public abstract class Animation {

    /// The cycle count of an animation that repeats until it is stopped.
    public static final int INDEFINITE = -1;

    /// What an animation is doing.
    public enum Status {
        /// Stopped for now; [Animation#play()] continues where it was.
        PAUSED,
        /// Playing.
        RUNNING,
        /// Not playing, and back at its start when it was stopped
        /// rather than finished.
        STOPPED
    }

    /// The animation this one is a child of, which then plays it.
    Animation parent;

    private final DoubleProperty rate = new SimpleDoubleProperty(this, "rate", 1.0);
    private final ReadOnlyDoubleWrapper currentRate = new ReadOnlyDoubleWrapper(this, "currentRate", 0.0);
    private final ReadOnlyObjectWrapper<Duration> cycleDuration =
            new ReadOnlyObjectWrapper<Duration>(this, "cycleDuration", Duration.ZERO);
    private final ReadOnlyObjectWrapper<Duration> totalDuration =
            new ReadOnlyObjectWrapper<Duration>(this, "totalDuration", Duration.ZERO);
    private final ReadOnlyObjectWrapper<Duration> currentTime =
            new ReadOnlyObjectWrapper<Duration>(this, "currentTime", Duration.ZERO);
    private final ObjectProperty<Duration> delay = new SimpleObjectProperty<Duration>(this, "delay", Duration.ZERO);
    private final IntegerProperty cycleCount = new SimpleIntegerProperty(this, "cycleCount", 1);
    private final BooleanProperty autoReverse = new SimpleBooleanProperty(this, "autoReverse", false);
    private final ReadOnlyObjectWrapper<Status> status =
            new ReadOnlyObjectWrapper<Status>(this, "status", Status.STOPPED);
    private final ObjectProperty<EventHandler<ActionEvent>> onFinished =
            new SimpleObjectProperty<EventHandler<ActionEvent>>(this, "onFinished");
    private ObservableMap<String, Duration> cuePoints;

    // What the animation was started with.
    private double cycleMillis;
    private int cycles = 1;
    private boolean reversing;

    // The playhead: the cycle it is in, and how far into that cycle it
    // is as the animation as a whole counts it, which runs opposite to
    // the content's own time in a reversed cycle.
    private long cycleIndex;
    private double cyclePos;

    private boolean startPending;
    private boolean jumpPending;
    private boolean lastPlayedFinished;
    private double delayLeft;
    private long lastPulseNanos;
    private boolean embeddedForward = true;

    /// Changes whenever the animation is started, paused, stopped or
    /// moved, so that a step in progress, whose handler did it, notices
    /// and ends.
    int token;

    private final FrameClock.Pulse pulse = new FrameClock.Pulse() {
        @Override
        public void pulse(long nowNanos) {
            onPulse(nowNanos);
        }
    };

    /// Creates an animation.
    protected Animation() {
    }

    // ---- properties ----

    /// Sets the speed and direction: 1 is normal speed, 2 twice as
    /// fast, a negative rate plays backwards.
    public final void setRate(double value) {
        rate.set(value);
        if (getStatus() == Status.RUNNING) {
            syncCurrentRate();
        }
    }

    /// Returns the speed and direction the animation is to play at.
    public final double getRate() {
        return rate.get();
    }

    /// The speed and direction the animation is to play at.
    public final DoubleProperty rateProperty() {
        return rate;
    }

    /// Returns the speed and direction the animation is playing at
    /// right now: 0 when it is not running, and of the opposite sign to
    /// the rate in a reversed cycle.
    public final double getCurrentRate() {
        return currentRate.get();
    }

    /// The speed and direction the animation is playing at right now.
    public final ReadOnlyDoubleProperty currentRateProperty() {
        return currentRate.getReadOnlyProperty();
    }

    /// Sets the length of one cycle.
    protected final void setCycleDuration(Duration value) {
        cycleDuration.set(value == null ? Duration.ZERO : value);
        syncTotalDuration();
    }

    /// Returns the length of one cycle.
    public final Duration getCycleDuration() {
        syncWhenStopped();
        return cycleDuration.get();
    }

    /// The length of one cycle.
    public final ReadOnlyObjectProperty<Duration> cycleDurationProperty() {
        syncWhenStopped();
        return cycleDuration.getReadOnlyProperty();
    }

    /// Returns the length of all cycles together, `Duration.INDEFINITE`
    /// for an animation that repeats until it is stopped.
    public final Duration getTotalDuration() {
        syncWhenStopped();
        return totalDuration.get();
    }

    /// The length of all cycles together.
    public final ReadOnlyObjectProperty<Duration> totalDurationProperty() {
        syncWhenStopped();
        return totalDuration.getReadOnlyProperty();
    }

    /// Returns the position of the playhead within its cycle.
    public final Duration getCurrentTime() {
        return currentTime.get();
    }

    /// The position of the playhead within its cycle.
    public final ReadOnlyObjectProperty<Duration> currentTimeProperty() {
        return currentTime.getReadOnlyProperty();
    }

    /// Sets the time the animation waits after it was started.
    public final void setDelay(Duration value) {
        if (value == null) {
            throw new NullPointerException("Delay must not be null");
        }
        if (value.lessThan(Duration.ZERO) || value.isUnknown()) {
            throw new IllegalArgumentException("Cannot set delay to negative value. Setting to Duration.ZERO");
        }
        delay.set(value);
    }

    /// Returns the time the animation waits after it was started.
    public final Duration getDelay() {
        return delay.get();
    }

    /// The time the animation waits after it was started.
    public final ObjectProperty<Duration> delayProperty() {
        return delay;
    }

    /// Sets how many cycles are played; [#INDEFINITE] repeats until the
    /// animation is stopped.
    public final void setCycleCount(int value) {
        if (value == 0 || value < INDEFINITE) {
            throw new IllegalArgumentException("Cycle count cannot be 0 or less than INDEFINITE");
        }
        cycleCount.set(value);
        syncTotalDuration();
    }

    /// Returns how many cycles are played.
    public final int getCycleCount() {
        return cycleCount.get();
    }

    /// How many cycles are played.
    public final IntegerProperty cycleCountProperty() {
        return cycleCount;
    }

    /// Sets whether every second cycle runs backwards.
    public final void setAutoReverse(boolean value) {
        autoReverse.set(value);
    }

    /// Returns whether every second cycle runs backwards.
    public final boolean isAutoReverse() {
        return autoReverse.get();
    }

    /// Whether every second cycle runs backwards.
    public final BooleanProperty autoReverseProperty() {
        return autoReverse;
    }

    /// Sets what the animation is doing.
    protected final void setStatus(Status value) {
        status.set(value);
    }

    /// Returns what the animation is doing.
    public final Status getStatus() {
        return status.get();
    }

    /// What the animation is doing.
    public final ReadOnlyObjectProperty<Status> statusProperty() {
        return status.getReadOnlyProperty();
    }

    /// Sets the handler that runs when the animation reaches its end by
    /// itself. It does not run when the animation is stopped, and never
    /// for one that repeats indefinitely.
    public final void setOnFinished(EventHandler<ActionEvent> value) {
        onFinished.set(value);
    }

    /// Returns the handler that runs when the animation finishes.
    public final EventHandler<ActionEvent> getOnFinished() {
        return onFinished.get();
    }

    /// The handler that runs when the animation finishes.
    public final ObjectProperty<EventHandler<ActionEvent>> onFinishedProperty() {
        return onFinished;
    }

    /// Returns the named positions of the animation, for
    /// [#jumpTo(String)] and [#playFrom(String)]. The names `"start"`
    /// and `"end"` always exist and need no entry.
    public final ObservableMap<String, Duration> getCuePoints() {
        if (cuePoints == null) {
            cuePoints = FXCollections.observableHashMap();
        }
        return cuePoints;
    }

    // ---- controls ----

    /// Plays from the current position in the direction of the rate. A
    /// paused animation continues; one that finished starts over from
    /// its start, or from its end when the rate is negative.
    public void play() {
        requireRoot("start");
        Status s = getStatus();
        if (s == Status.PAUSED) {
            token++;
            lastPulseNanos = FrameClock.nowNanos();
            setStatus(Status.RUNNING);
            syncCurrentRate();
            FrameClock.add(pulse);
        } else if (s == Status.STOPPED) {
            token++;
            double position = position();
            snapshot();
            if (lastPlayedFinished) {
                place(getRate() < 0 ? totalMillis() : 0);
                jumpPending = false;
            } else {
                place(position);
            }
            lastPlayedFinished = false;
            delayLeft = millis(getDelay());
            startPending = atEntryEdge(getRate() >= 0);
            doStart(true);
            syncCurrentTime();
            lastPulseNanos = FrameClock.nowNanos();
            setStatus(Status.RUNNING);
            syncCurrentRate();
            FrameClock.add(pulse);
        }
    }

    /// Plays forwards from the start.
    public void playFromStart() {
        stop();
        setRate(Math.abs(getRate()));
        jumpTo(Duration.ZERO);
        play();
    }

    /// Plays from a position.
    public void playFrom(Duration time) {
        jumpTo(time);
        play();
    }

    /// Plays from a cue point.
    public void playFrom(String cuePoint) {
        jumpTo(cuePoint);
        play();
    }

    /// Pauses a running animation where it is.
    public void pause() {
        requireRoot("pause");
        if (getStatus() == Status.RUNNING) {
            token++;
            FrameClock.remove(pulse);
            setStatus(Status.PAUSED);
            currentRate.set(0.0);
        }
    }

    /// Stops the animation and moves the playhead back to the start.
    /// The values the animation has written stay as they are, and the
    /// finished handler does not run.
    public void stop() {
        requireRoot("stop");
        if (getStatus() != Status.STOPPED) {
            token++;
            FrameClock.remove(pulse);
            doStop();
            setStatus(Status.STOPPED);
            currentRate.set(0.0);
            place(0);
            syncCurrentTime();
            jumpPending = false;
            lastPlayedFinished = true;
        }
    }

    /// Moves the playhead to a position counted from the start of the
    /// first cycle; a position outside the animation is moved to its
    /// nearer end. A stopped animation shows the position once it is
    /// played.
    public void jumpTo(Duration time) {
        if (time == null) {
            throw new NullPointerException("Time needs to be specified.");
        }
        if (time.isUnknown()) {
            throw new IllegalArgumentException("The time is invalid");
        }
        requireRoot("jump");
        boolean stopped = getStatus() == Status.STOPPED;
        if (stopped) {
            snapshot();
        }
        token++;
        double total = totalMillis();
        double target = time.isIndefinite() ? (Double.isInfinite(total) ? cycleMillis : total)
                : time.toMillis();
        place(target);
        lastPlayedFinished = false;
        startPending = false;
        syncCurrentTime();
        if (stopped) {
            jumpPending = true;
        } else {
            doJumpTo(localTime());
            if (getStatus() == Status.RUNNING) {
                syncCurrentRate();
            }
        }
    }

    /// Moves the playhead to a cue point: `"start"`, `"end"` or an
    /// entry of [#getCuePoints()]. An unknown name does nothing.
    public void jumpTo(String cuePoint) {
        if (cuePoint == null) {
            throw new NullPointerException("CuePoint needs to be specified");
        }
        if ("start".equalsIgnoreCase(cuePoint)) {
            jumpTo(Duration.ZERO);
        } else if ("end".equalsIgnoreCase(cuePoint)) {
            jumpTo(getTotalDuration());
        } else {
            Duration target = getCuePoints().get(cuePoint);
            if (target != null) {
                jumpTo(target);
            }
        }
    }

    // ---- what a kind of animation provides ----

    /// Called when the animation starts or, as a child, is entered.
    /// With `capture` set it starts afresh and reads what it needs from
    /// its targets; without, a parent is entering it again on a later
    /// cycle.
    abstract void doStart(boolean capture);

    /// Moves the content from one time of its cycle to another, running
    /// the handlers of what lies between. `atStart` says the cycle is
    /// being entered at `from`, so that what lies exactly there counts
    /// too.
    abstract void doPlayTo(double from, double to, boolean forward, boolean atStart);

    /// Shows the content at a time of its cycle, running no handlers.
    abstract void doJumpTo(double time);

    /// Called when the animation is stopped.
    void doStop() {
    }

    /// Brings the cycle duration up to date with what it derives from.
    void syncDuration() {
    }

    // ---- the engine ----

    private void requireRoot(String what) {
        if (parent != null) {
            throw new IllegalStateException("Cannot " + what + " when embedded in another animation");
        }
    }

    /// Returns the animation at the top of the tree this one is in.
    final Animation root() {
        Animation a = this;
        while (a.parent != null) {
            a = a.parent;
        }
        return a;
    }

    private void syncWhenStopped() {
        if (root().getStatus() == Status.STOPPED) {
            syncDuration();
        }
    }

    private static double millis(Duration d) {
        if (d == null || d.isUnknown()) {
            return 0;
        }
        double ms = d.toMillis();
        return ms < 0 ? 0 : ms;
    }

    private void syncTotalDuration() {
        Duration cycle = cycleDuration.get();
        int count = cycleCount.get();
        Duration total;
        if (count == INDEFINITE) {
            total = Duration.INDEFINITE;
        } else if (count <= 1) {
            total = cycle;
        } else {
            total = cycle.multiply(count);
        }
        totalDuration.set(total);
    }

    private void snapshot() {
        syncDuration();
        syncTotalDuration();
        cycleMillis = millis(cycleDuration.get());
        int count = cycleCount.get();
        cycles = count == INDEFINITE ? INDEFINITE : Math.max(1, count);
        reversing = autoReverse.get();
    }

    /// Returns the length of the cycle the animation was started with,
    /// in milliseconds.
    final double cycleMillis() {
        return cycleMillis;
    }

    private double totalMillis() {
        return cycles == INDEFINITE ? Double.POSITIVE_INFINITY : cycleMillis * cycles;
    }

    private double position() {
        return cycleIndex * cycleMillis + cyclePos;
    }

    private void place(double position) {
        if (cycleMillis <= 0 || Double.isInfinite(cycleMillis)) {
            cycleIndex = 0;
            cyclePos = cycleMillis <= 0 || position < 0 ? 0 : position;
            return;
        }
        double p = position < 0 ? 0 : Math.min(position, totalMillis());
        long index = (long) Math.floor(p / cycleMillis);
        if (cycles != INDEFINITE && index >= cycles) {
            index = cycles - 1L;
        }
        cycleIndex = index;
        cyclePos = Math.max(0, Math.min(cycleMillis, p - index * cycleMillis));
    }

    private boolean reversedCycle() {
        return reversing && (cycleIndex & 1L) == 1L;
    }

    /// Returns the playhead as the content counts it, within its cycle.
    final double localMillis() {
        return localTime();
    }

    private double localTime() {
        return reversedCycle() ? cycleMillis - cyclePos : cyclePos;
    }

    private boolean atEntryEdge(boolean forward) {
        return forward ? cyclePos <= 0 : cyclePos >= cycleMillis;
    }

    private void syncCurrentTime() {
        currentTime.set(Duration.millis(localTime()));
    }

    private void syncCurrentRate() {
        double r = getRate();
        currentRate.set(reversedCycle() ? -r : r);
    }

    private void onPulse(long nowNanos) {
        double elapsed = (nowNanos - lastPulseNanos) / 1000000.0;
        lastPulseNanos = nowNanos;
        if (elapsed < 0) {
            elapsed = 0;
        }
        if (delayLeft > 0) {
            if (elapsed < delayLeft) {
                delayLeft -= elapsed;
                return;
            }
            elapsed -= delayLeft;
            delayLeft = 0;
        }
        int started = token;
        if (jumpPending) {
            jumpPending = false;
            doJumpTo(localTime());
            if (token != started) {
                return;
            }
        }
        double r = getRate();
        boolean finished = advance(elapsed * Math.abs(r), r >= 0);
        if (token != started) {
            return;
        }
        if (finished) {
            token++;
            FrameClock.remove(pulse);
            setStatus(Status.STOPPED);
            currentRate.set(0.0);
            lastPlayedFinished = true;
            fireFinished();
        } else {
            syncCurrentRate();
        }
    }

    private void fireFinished() {
        EventHandler<ActionEvent> handler = getOnFinished();
        if (handler != null) {
            handler.handle(new ActionEvent(this, null));
        }
    }

    /// Moves the playhead by an amount of time in a direction, cycle by
    /// cycle so that nothing in between is skipped, and returns whether
    /// it reached the end of the animation in that direction.
    private boolean advance(double amount, boolean forward) {
        Animation top = root();
        int started = top.token;
        if (cycleMillis <= 0) {
            boolean entering = startPending;
            startPending = false;
            doPlayTo(0, 0, forward, entering);
            return top.token == started;
        }
        double remaining = amount;
        while (true) {
            boolean localForward = forward != reversedCycle();
            double from = localTime();
            boolean entering = startPending;
            startPending = false;
            double room = forward ? cycleMillis - cyclePos : cyclePos;
            if (remaining < room) {
                cyclePos += forward ? remaining : -remaining;
                syncCurrentTime();
                doPlayTo(from, localTime(), localForward, entering);
                return false;
            }
            cyclePos = forward ? cycleMillis : 0;
            remaining -= room;
            syncCurrentTime();
            doPlayTo(from, localTime(), localForward, entering);
            if (top.token != started) {
                return false;
            }
            boolean last = forward ? cycles != INDEFINITE && cycleIndex >= cycles - 1L : cycleIndex <= 0;
            if (last) {
                return true;
            }
            if (remaining <= 0) {
                // Exactly on the boundary: the cycle that ended is still the
                // one showing, and the next frame enters the next one.
                return false;
            }
            cycleIndex += forward ? 1 : -1;
            cyclePos = forward ? 0 : cycleMillis;
            // A reversed cycle begins where the last one ended, which was
            // visited a moment ago; any other begins at the far edge.
            startPending = !reversing;
            if (parent == null) {
                syncCurrentRate();
            }
        }
    }

    // ---- being played by a parent ----

    /// Returns how long this animation takes as a child: its delay and
    /// all its cycles at its own rate.
    final double embeddedSpan() {
        snapshot();
        double speed = Math.abs(getRate());
        double total = totalMillis();
        double wait = millis(getDelay());
        if (speed <= 0) {
            return wait;
        }
        return wait + total / speed;
    }

    /// Places the child at the edge its parent enters it from.
    final void embeddedEnter(boolean parentForward, boolean capture) {
        snapshot();
        boolean forward = parentForward == (getRate() >= 0);
        double total = totalMillis();
        place(forward || Double.isInfinite(total) ? 0 : total);
        embeddedForward = forward;
        startPending = true;
        jumpPending = false;
        doStart(capture);
        syncCurrentTime();
    }

    private double embeddedTarget(double elapsed) {
        double speed = Math.abs(getRate());
        double played = Math.max(0, elapsed - millis(getDelay())) * speed;
        double total = totalMillis();
        if (played > total) {
            played = total;
        }
        return getRate() < 0 && !Double.isInfinite(total) ? total - played : played;
    }

    /// Plays the child to the time its parent has reached within it,
    /// running its handlers, the finished handler included when it gets
    /// to its end.
    final void embeddedPlayTo(double elapsed) {
        Animation top = root();
        int started = top.token;
        double target = embeddedTarget(elapsed);
        double position = position();
        if (target > position) {
            embeddedForward = true;
        } else if (target < position) {
            embeddedForward = false;
        }
        boolean finished = advance(Math.abs(target - position), embeddedForward);
        if (finished && top.token == started) {
            fireFinished();
        }
    }

    /// Shows the child at the time its parent has jumped to within it.
    final void embeddedJumpTo(double elapsed) {
        place(embeddedTarget(elapsed));
        startPending = false;
        syncCurrentTime();
        doJumpTo(localTime());
    }
}
