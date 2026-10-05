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
package androidx.lifecycle;

import java.util.ArrayList;
import java.util.List;

/// A `Lifecycle` driven by its owner. Every observer is moved through each
/// state in order, one event at a time, as AndroidX does: an observer added
/// late catches up from `INITIALIZED`, and observers added earlier hear an
/// event before later ones going up and after them going down.
public class LifecycleRegistry extends Lifecycle {

    private static final class Entry {
        final LifecycleObserver observer;
        State state;

        Entry(LifecycleObserver observer, State state) {
            this.observer = observer;
            this.state = state;
        }
    }

    private final LifecycleOwner owner;
    private final List<Entry> entries = new ArrayList<Entry>();
    private State state = State.INITIALIZED;

    public LifecycleRegistry(LifecycleOwner provider) {
        this.owner = provider;
    }

    @Override
    public State getCurrentState() {
        return state;
    }

    public void setCurrentState(State next) {
        moveTo(next);
    }

    /// Deprecated in AndroidX, kept for applications that still call it.
    public void markState(State next) {
        moveTo(next);
    }

    public void handleLifecycleEvent(Event event) {
        moveTo(event.getTargetState());
    }

    public int getObserverCount() {
        return entries.size();
    }

    @Override
    public void addObserver(LifecycleObserver observer) {
        for (Entry e : entries) {
            if (e.observer == observer) {
                return;
            }
        }
        Entry e = new Entry(observer, state == State.DESTROYED ? State.DESTROYED : State.INITIALIZED);
        entries.add(e);
        catchUp(e);
    }

    @Override
    public void removeObserver(LifecycleObserver observer) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).observer == observer) {
                entries.remove(i);
                return;
            }
        }
    }

    private void moveTo(State next) {
        if (state == next) {
            return;
        }
        if (state == State.INITIALIZED && next == State.DESTROYED) {
            throw new IllegalStateException("no event down from INITIALIZED");
        }
        state = next;
        sync();
    }

    private void sync() {
        // Down in reverse order of addition, up in order, as AndroidX.
        // Over a snapshot: an observer may remove itself, or another, from
        // its callback, and indexing the live list then skipped the next
        // observer (or revisited one). catchUp and goDown pass over an entry
        // that is no longer registered; one added meanwhile caught up in
        // addObserver.
        if (state.compareTo(State.CREATED) < 0 || anyAbove()) {
            List<Entry> down = new ArrayList<Entry>(entries);
            for (int i = down.size() - 1; i >= 0; i--) {
                goDown(down.get(i));
            }
        }
        List<Entry> up = new ArrayList<Entry>(entries);
        for (int i = 0; i < up.size(); i++) {
            catchUp(up.get(i));
        }
    }

    private boolean anyAbove() {
        for (Entry e : entries) {
            if (e.state.compareTo(state) > 0) {
                return true;
            }
        }
        return false;
    }

    private void catchUp(Entry e) {
        while (e.state.compareTo(state) < 0 && entries.contains(e)) {
            Event up = Event.upFrom(e.state);
            if (up == null) {
                return;
            }
            dispatch(e, up);
        }
    }

    private void goDown(Entry e) {
        while (e.state.compareTo(state) > 0 && entries.contains(e)) {
            Event down = Event.downFrom(e.state);
            if (down == null) {
                return;
            }
            dispatch(e, down);
        }
    }

    private void dispatch(Entry e, Event event) {
        e.state = event.getTargetState();
        LifecycleObserver o = e.observer;
        if (o instanceof DefaultLifecycleObserver) {
            DefaultLifecycleObserver d = (DefaultLifecycleObserver) o;
            switch (event) {
                case ON_CREATE:
                    d.onCreate(owner);
                    break;
                case ON_START:
                    d.onStart(owner);
                    break;
                case ON_RESUME:
                    d.onResume(owner);
                    break;
                case ON_PAUSE:
                    d.onPause(owner);
                    break;
                case ON_STOP:
                    d.onStop(owner);
                    break;
                case ON_DESTROY:
                    d.onDestroy(owner);
                    break;
                default:
                    break;
            }
        }
        if (o instanceof LifecycleEventObserver) {
            ((LifecycleEventObserver) o).onStateChanged(owner, event);
        }
    }
}
