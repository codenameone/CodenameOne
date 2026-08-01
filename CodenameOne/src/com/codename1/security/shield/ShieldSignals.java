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
package com.codename1.security.shield;

import com.codename1.ui.Display;
import java.util.Vector;

/// Collection point for runtime self-protection observations.
///
/// The framework's own detections report here, and so can a cn1lib or the app itself when it
/// notices something the platform checks cannot see -- a failed server-side consistency check, a
/// suspicious sequence of user actions. Everything recorded here is offered to the attestation
/// service on the next token fetch, where the policy engine decides what it means.
///
/// The bus is bounded: it keeps only the most recent observations, and repeat reports of the same
/// id collapse onto the existing entry rather than accumulating. A hooking framework that trips a
/// detector on every frame must not be able to exhaust memory.
public final class ShieldSignals {

    private static final int MAX_SIGNALS = 32;

    private static final Vector signals = new Vector();
    private static final Vector listeners = new Vector();

    private ShieldSignals() {
    }

    /// Records an observation. Repeat reports of an id already present update that entry in place.
    /// Safe to call from any thread; listeners are notified on the EDT.
    ///
    /// An identical repeat still updates the stored entry -- the snapshot is meant to hold
    /// the most recent observation of each signal -- but does not notify again.
    public static void add(ShieldSignal signal) {
        if (signal == null || signal.getId() == null) {
            return;
        }
        synchronized (signals) {
            boolean replaced = false;
            for (int i = 0; i < signals.size(); i++) {
                ShieldSignal existing = (ShieldSignal) signals.elementAt(i);
                if (existing.getId().equals(signal.getId())) {
                    // Nothing new to say. Re-reporting an identical observation is the
                    // normal case, not an edge one: AppShield.getSignals() re-adds
                    // everything collectSignals() returns, so a listener that refreshes
                    // its view by calling getSignals() notified itself, forever. Even
                    // without that, a detector polling on a timer queued a runnable per
                    // poll per signal onto the EDT -- an unbounded queue behind a bus
                    // whose whole selling point is that it is bounded.
                    boolean sameObservation = existing.getSeverity() == signal.getSeverity()
                            && sameDetail(existing.getDetail(), signal.getDetail());
                    // The entry is replaced either way, and only the NOTIFICATION is
                    // suppressed. Keeping the old object was a second bug hiding behind
                    // the first: this bus documents itself as holding the most recent
                    // observation of each signal, and a persistent one -- a root, a
                    // hooking framework -- is re-reported on every poll, so the entry the
                    // engine and the server were shown kept the timestamp of the first
                    // sighting hours after the fact. "When did this device last look
                    // compromised" is a question the answer is used for.
                    signals.setElementAt(signal, i);
                    replaced = true;
                    if (sameObservation) {
                        return;
                    }
                    break;
                }
            }
            if (!replaced) {
                if (signals.size() >= MAX_SIGNALS) {
                    signals.removeElementAt(0);
                }
                signals.addElement(signal);
            }
        }
        // Outside the lock on every path. Display.callSerially runs the task
        // inline when the EDT is not up yet, so a listener that calls back into
        // snapshot() would deadlock on the monitor we were still holding.
        notifyListeners(signal);
    }

    /// Whether two observations of one signal say the same thing.
    ///
    /// The detail is what distinguishes "accessibility service X" from "service Y" under
    /// one id, so a change in it is a new observation and a repeat of it is not.
    private static boolean sameDetail(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    /// Convenience overload for the common case.
    public static void add(String id, int severity, String detail) {
        add(new ShieldSignal(id, severity, detail));
    }

    /// The observations recorded so far. Never null.
    public static ShieldSignal[] snapshot() {
        synchronized (signals) {
            ShieldSignal[] out = new ShieldSignal[signals.size()];
            signals.copyInto(out);
            return out;
        }
    }

    /// True when any recorded observation is at or above the given severity.
    public static boolean hasSignalAtLeast(int severity) {
        synchronized (signals) {
            for (int i = 0; i < signals.size(); i++) {
                if (((ShieldSignal) signals.elementAt(i)).getSeverity() >= severity) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Discards every recorded observation. Intended for tests and for the simulator's
    /// signal-faking menu.
    public static void clear() {
        synchronized (signals) {
            signals.removeAllElements();
        }
    }

    static void addListener(ShieldListener l) {
        if (l == null) {
            return;
        }
        synchronized (listeners) {
            if (!listeners.contains(l)) {
                listeners.addElement(l);
            }
        }
    }

    static void removeListener(ShieldListener l) {
        synchronized (listeners) {
            listeners.removeElement(l);
        }
    }

    private static void notifyListeners(ShieldSignal signal) {
        ShieldListener[] copy;
        synchronized (listeners) {
            if (listeners.isEmpty()) {
                return;
            }
            copy = new ShieldListener[listeners.size()];
            listeners.copyInto(copy);
        }
        Display.getInstance().callSerially(new SignalDispatch(copy, signal));
    }

    private static final class SignalDispatch implements Runnable {
        private final ShieldListener[] targets;
        private final ShieldSignal signal;

        SignalDispatch(ShieldListener[] targets, ShieldSignal signal) {
            this.targets = targets;
            this.signal = signal;
        }

        @Override
        public void run() {
            for (ShieldListener target : targets) {
                target.signalRaised(signal);
            }
        }
    }
}
