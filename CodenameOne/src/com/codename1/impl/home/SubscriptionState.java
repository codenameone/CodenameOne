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
package com.codename1.impl.home;

import com.codename1.home.HomeChangeListener;
import com.codename1.home.TraitChangeBatch;
import com.codename1.home.TraitReading;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/// One live subscription: its listener, the changes gathered since the last
/// delivery, and the timer that decides when to hand them over.
///
/// #### Why coalescing is the framework's job and not the port's
///
/// A dragged dimmer emits a notification per step, and a home with a hundred
/// watched accessories produces a steady stream. Delivering each one to the
/// EDT is how a list of lights becomes unscrollable while somebody is
/// adjusting one.
///
/// Doing it here rather than in each bridge means the three ports do not each
/// have to get it right, and means the window is the caller's choice rather
/// than a per-platform accident. Within the window, changes are collapsed per
/// accessory, service and trait, keeping the newest -- so a batch is a state
/// update, never an event log.
///
/// A window of zero delivers each change on its own, still on the EDT.
public final class SubscriptionState {

    /// One timer thread shared by every subscription in the process, created
    /// on first need and released when the last subscription goes.
    ///
    /// A timer per subscription would be a thread per watched screen, and
    /// CLDC's `Timer` has no daemon constructor -- one left running keeps the
    /// VM alive on the ports where that matters.
    private static Timer sharedTimer;
    private static int sharedTimerUsers;

    private final String id;
    private final HomeChangeListener listener;
    private final int windowMillis;

    /// Insertion-ordered so a delivery arrives in the order the accessories
    /// changed, which is the order a UI most naturally animates.
    private final Map<String, TraitReading> pending =
            new LinkedHashMap<String, TraitReading>();

    private boolean flushArmed;
    private boolean resyncRequired;
    private boolean disposed;
    /// True while the up-front read of current values is still in flight.
    ///
    /// Live changes are held rather than delivered during that window. The
    /// read snapshots its values when it starts, so a change that happens
    /// after it started but is delivered before it finishes would be
    /// overwritten on screen by the older snapshot -- a light that turns on
    /// and then shows itself off, from a subscription whose whole job is to
    /// keep the screen true.
    private boolean awaitingInitial;

    /// Live readings held for the initial delivery when the window is zero.
    ///
    /// A list rather than the keyed map, because a zero window promises every
    /// step and the map keeps only the newest per trait. Empty whenever the
    /// window is non-zero: there, coalescing is what was asked for.
    private final List<TraitReading> heldSteps =
            new ArrayList<TraitReading>();

    /// Creates a subscription's state and claims a share of the timer.
    ///
    /// #### Parameters
    ///
    /// - `id`: the subscription identifier
    ///
    /// - `listener`: where batches are delivered
    ///
    /// - `windowMillis`: the coalescing window; zero to deliver everything
    public SubscriptionState(String id, HomeChangeListener listener,
            int windowMillis) {
        this(id, listener, windowMillis, false);
    }

    /// Creates a subscription's state, claims a share of the timer, and says
    /// whether an up-front read is coming.
    ///
    /// #### Parameters
    ///
    /// - `id`: the subscription identifier
    ///
    /// - `listener`: where batches are delivered
    ///
    /// - `windowMillis`: the coalescing window; zero to deliver everything
    ///
    /// - `expectsInitial`: whether current values will be delivered up front
    public SubscriptionState(String id, HomeChangeListener listener,
            int windowMillis, boolean expectsInitial) {
        this.id = id;
        this.listener = listener;
        this.windowMillis = windowMillis;
        this.awaitingInitial = expectsInitial;
        if (windowMillis > 0) {
            acquireTimer();
        }
    }

    private static synchronized Timer acquireTimer() {
        if (sharedTimer == null) {
            sharedTimer = new Timer();
        }
        sharedTimerUsers++;
        return sharedTimer;
    }

    private static synchronized void releaseTimer() {
        sharedTimerUsers--;
        if (sharedTimerUsers <= 0) {
            sharedTimerUsers = 0;
            if (sharedTimer != null) {
                sharedTimer.cancel();
                sharedTimer = null;
            }
        }
    }

    private static synchronized Timer timer() {
        return sharedTimer;
    }

    /// The subscription this belongs to.
    ///
    /// #### Returns
    ///
    /// the identifier
    public String getId() {
        return id;
    }

    /// Takes in changes from the port and schedules a delivery.
    ///
    /// Safe from any thread: the platform callbacks that produce these arrive
    /// on whatever thread the OS uses, and on iOS that is the Objective-C main
    /// queue, which is not the Codename One EDT.
    ///
    /// #### Parameters
    ///
    /// - `readings`: the changed values
    ///
    /// - `initial`: whether this is the up-front delivery of current values
    public void offer(List<TraitReading> readings, boolean initial) {
        if (readings == null || readings.isEmpty()) {
            return;
        }
        if (initial) {
            // Not coalesced and not delayed. The point of the initial
            // delivery is to fill a screen, and holding it for the window
            // would leave that screen blank for no reason.
            //
            // The disposed check matters most here: the read that produces
            // the initial values is in flight while the caller is free to
            // stop(), and a form torn down in between would otherwise still
            // be handed a batch after the subscription said it had stopped.
            boolean held;
            List<TraitReading> steps = null;
            synchronized (this) {
                if (disposed) {
                    return;
                }
                awaitingInitial = false;
                // resyncRequired counts as something held: markResyncRequired
                // deliberately does not deliver while the initial read is in
                // flight, so if nothing else is pending this is the only
                // chance to hand the flag over. Without it a subscription
                // whose notification registration failed during its own
                // initial read delivered those values looking perfectly
                // healthy and never said they could not be trusted.
                held = !pending.isEmpty() || resyncRequired;
                if (!heldSteps.isEmpty()) {
                    steps = new ArrayList<TraitReading>(heldSteps);
                    heldSteps.clear();
                }
            }
            dispatch(new TraitChangeBatch(id, readings, true, false));
            // Changes that arrived while the read was in flight. They are
            // newer than what it snapshotted, so they go out immediately
            // after it rather than waiting for the window -- otherwise the
            // older values are the last thing the screen sees.
            if (held) {
                flush();
            }
            if (steps != null) {
                dispatch(new TraitChangeBatch(id, steps, false, false));
            }
            return;
        }
        boolean arm = false;
        boolean hold;
        synchronized (this) {
            if (disposed) {
                return;
            }
            hold = awaitingInitial;
            if (windowMillis <= 0) {
                // A window of zero means every step, and this delivery can
                // carry several for one trait -- a drain of writes that
                // accumulated while nobody was looking. Keyed into `pending`
                // they collapse to the last one, which is the one thing a
                // zero window promises not to do. Held for the initial read
                // in a list for the same reason.
                arm = false;
                if (hold) {
                    heldSteps.addAll(readings);
                }
            } else {
                for (TraitReading r : readings) {
                    pending.put(keyOf(r), r);
                }
                if (hold) {
                    arm = false;
                } else if (!flushArmed) {
                    flushArmed = true;
                    arm = true;
                }
            }
        }
        if (hold) {
            // Held, not dropped: the initial delivery flushes them.
            return;
        }
        if (windowMillis <= 0) {
            // Whatever was already pending goes first, so a resync flag or a
            // batch held for the initial read keeps its place in the order.
            flush();
            dispatch(new TraitChangeBatch(id, readings, false, false));
            return;
        }
        if (arm) {
            Timer t = timer();
            if (t == null) {
                // The shared timer is gone, which means every subscription was
                // disposed while this delivery was in flight. Hand it over
                // immediately rather than dropping it.
                flush();
                return;
            }
            try {
                t.schedule(new Flush(this), windowMillis);
            } catch (IllegalStateException timerAlreadyCancelled) {
                // Raced with the last subscription being disposed. Same
                // answer: deliver now rather than never.
                flush();
            }
        }
    }

    /// Says the up-front read produced nothing, so nothing is coming.
    ///
    /// Whatever is being held for it is released now. Without this, a read
    /// that failed -- an accessory unreachable at exactly the wrong moment --
    /// would hold every live change for the life of the subscription, which
    /// is a far worse outcome than the missing initial values.
    public void initialDeliveryUnavailable() {
        boolean release;
        List<TraitReading> steps = null;
        synchronized (this) {
            if (disposed || !awaitingInitial) {
                return;
            }
            awaitingInitial = false;
            release = !pending.isEmpty() || resyncRequired;
            if (!heldSteps.isEmpty()) {
                steps = new ArrayList<TraitReading>(heldSteps);
                heldSteps.clear();
            }
        }
        if (release) {
            flush();
        }
        if (steps != null) {
            dispatch(new TraitChangeBatch(id, steps, false, false));
        }
    }

    /// Records that the platform lost its notification stream, so the next
    /// batch tells the listener its values are stale.
    public void markResyncRequired() {
        boolean deliverNow;
        synchronized (this) {
            if (disposed) {
                return;
            }
            resyncRequired = true;
            deliverNow = !flushArmed && pending.isEmpty() && !awaitingInitial;
        }
        if (deliverNow) {
            // Nothing was pending, so no flush is coming to carry the flag.
            // An empty batch is delivered rather than the flag being held
            // until the next change, which might be much later or never --
            // and a UI showing values from before the gap with nothing to
            // indicate it is exactly what this flag exists to prevent.
            flush();
        }
    }

    private void flush() {
        List<TraitReading> batch;
        boolean resync;
        synchronized (this) {
            if (disposed) {
                return;
            }
            flushArmed = false;
            resync = resyncRequired;
            resyncRequired = false;
            if (pending.isEmpty() && !resync) {
                return;
            }
            batch = new ArrayList<TraitReading>(pending.values());
            pending.clear();
        }
        dispatch(new TraitChangeBatch(id, batch, false, resync));
    }

    private void dispatch(final TraitChangeBatch batch) {
        if (Display.isInitialized() && !Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Deliver(this, batch));
            return;
        }
        if (isDisposed()) {
            return;
        }
        listener.traitsChanged(batch);
    }

    private synchronized boolean isDisposed() {
        return disposed;
    }

    /// Stops delivering and releases this subscription's share of the timer.
    /// Idempotent.
    public void dispose() {
        boolean wasUsingTimer;
        synchronized (this) {
            if (disposed) {
                return;
            }
            disposed = true;
            pending.clear();
            heldSteps.clear();
            wasUsingTimer = windowMillis > 0;
        }
        if (wasUsingTimer) {
            releaseTimer();
        }
    }

    private static String keyOf(TraitReading r) {
        return r.getAccessoryId() + "\t" + r.getServiceId() + "\t"
                + r.getTrait().getId();
    }

    /// Named rather than anonymous so the scheduled task carries no synthetic
    /// reference to anything enclosing (SpotBugs
    /// `SIC_INNER_SHOULD_BE_STATIC_ANON`).
    private static final class Flush extends TimerTask {

        private final SubscriptionState state;

        Flush(SubscriptionState state) {
            this.state = state;
        }

        @Override
        public void run() {
            state.flush();
        }
    }

    /// The EDT hop for one batch.
    ///
    /// It re-checks `disposed` when it runs rather than trusting the check
    /// made when it was queued: the queue is where a `stop()` on the EDT most
    /// easily overtakes a batch produced on a platform thread, and delivering
    /// to a form that has already been torn down is what stopping is for.
    private static final class Deliver implements Runnable {

        private final SubscriptionState state;
        private final TraitChangeBatch batch;

        Deliver(SubscriptionState state, TraitChangeBatch batch) {
            this.state = state;
            this.batch = batch;
        }

        @Override
        public void run() {
            if (state.isDisposed()) {
                return;
            }
            state.listener.traitsChanged(batch);
        }
    }
}
