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

import com.codename1.flutter.foundation.Listenable;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * A value of type {@code T} that changes over the lifetime of an animation,
 * plus a {@link AnimationStatus} and listener registration — Flutter's
 * {@code Animation<T>}.
 *
 * <p>Concrete subclasses ({@link AnimationController}, {@link CurvedAnimation},
 * {@link AlwaysStoppedAnimation}, and the tween-driven evaluations) supply the
 * current {@link #value()} and {@link #status()}. This base class owns the
 * value- and status-listener bookkeeping so subclasses can broadcast changes
 * with {@link #notifyListeners()} / {@link #notifyStatusListeners}.</p>
 */
public abstract class Animation<T> implements Listenable {

    private final List<Funcs.VoidFunc0> listeners = new ArrayList<Funcs.VoidFunc0>();
    private final List<Funcs.VoidFunc1<AnimationStatus>> statusListeners =
            new ArrayList<Funcs.VoidFunc1<AnimationStatus>>();

    /** The current animated value. */
    public abstract T value();

    /** The current phase; the default is {@link AnimationStatus#dismissed}. */
    public AnimationStatus status() {
        return AnimationStatus.dismissed;
    }

    public boolean isCompleted() {
        return status() == AnimationStatus.completed;
    }

    public boolean isDismissed() {
        return status() == AnimationStatus.dismissed;
    }

    public boolean isAnimating() {
        AnimationStatus s = status();
        return s == AnimationStatus.forward || s == AnimationStatus.reverse;
    }

    public boolean isForwardOrCompleted() {
        AnimationStatus s = status();
        return s == AnimationStatus.forward || s == AnimationStatus.completed;
    }

    public void addListener(Funcs.VoidFunc0 listener) {
        if (listener != null) {
            boolean first = !hasAnyListeners();
            listeners.add(listener);
            if (first) {
                didStartListening();
            }
        }
    }

    public void removeListener(Funcs.VoidFunc0 listener) {
        boolean had = hasAnyListeners();
        listeners.remove(listener);
        if (had && !hasAnyListeners()) {
            didStopListening();
        }
    }

    public void addStatusListener(Funcs.VoidFunc1<AnimationStatus> listener) {
        if (listener != null) {
            boolean first = !hasAnyListeners();
            statusListeners.add(listener);
            if (first) {
                didStartListening();
            }
        }
    }

    public void removeStatusListener(Funcs.VoidFunc1<AnimationStatus> listener) {
        boolean had = hasAnyListeners();
        statusListeners.remove(listener);
        if (had && !hasAnyListeners()) {
            didStopListening();
        }
    }

    /** Whether anyone listens to this animation's value or status. */
    protected final boolean hasAnyListeners() {
        return !listeners.isEmpty() || !statusListeners.isEmpty();
    }

    /**
     * The first listener arrived: an animation derived from another subscribes to it
     * here, as Flutter's AnimationLazyListenerMixin does, so it forwards changes only
     * while someone is listening and never holds its parent's listener list otherwise.
     */
    protected void didStartListening() {
    }

    /** The last listener left: undo {@link #didStartListening()}. */
    protected void didStopListening() {
    }

    protected void notifyListeners() {
        com.codename1.flutter.foundation.Listeners.notify(listeners);
    }

    protected void notifyStatusListeners(AnimationStatus s) {
        com.codename1.flutter.foundation.Listeners.notify(statusListeners, s);
    }

    /** Removes every value and status listener, as Flutter's dispose does. */
    protected void clearListeners() {
        boolean had = hasAnyListeners();
        listeners.clear();
        statusListeners.clear();
        if (had) {
            didStopListening();
        }
    }

    /**
     * Drives {@code child} from this animation (which must produce doubles):
     * {@code controller.drive(tween)} == {@code tween.animate(controller)}.
     */
    @SuppressWarnings("unchecked")
    public <R> Animation<R> drive(Animatable<R> child) {
        return child.animate((Animation<Double>) (Animation<?>) this);
    }
}
