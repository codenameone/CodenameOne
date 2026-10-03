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
package com.codename1.flutter.widgets;

import com.codename1.flutter.animation.Curve;

import dart.async.Future;
import dart.core.Duration;
import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * Controls the offset of a scrollable ({@code ScrollController} in Flutter). The
 * new_gallery desktop carousel reads {@link #offset()} / {@link #position()},
 * calls {@link #animateTo}, and adds a listener to toggle its paging buttons.
 *
 * <p>The controller owns a {@link ScrollPosition}; the mounted scroll render
 * element keeps that position's extents in sync and forwards user scrolls, which
 * notify listeners.</p>
 */
public class ScrollController {

    /// The controller's own position: what offset/position report with no scrollable
    /// attached, and the position the first scrollable to attach takes over.
    private final ScrollPosition scrollPosition = new ScrollPosition();
    private final List<Funcs.VoidFunc0> listeners = new ArrayList<Funcs.VoidFunc0>();
    private double initialScrollOffset;
    private boolean keepScrollOffset = true;
    private String debugLabel;
    /// Set by the client-less {@link #attach()}.
    private boolean attached;

    /// One attached scrollable and its position. Flutter's controller keeps a LIST of
    /// positions -- a controller shared by two lists drives both -- and this used to keep
    /// one client, so attaching a second silently detached the first.
    private static final class Attachment {
        final Client client;
        final ScrollPosition position;
        /// Whether the animateTo in flight is still driving this one.
        boolean driven;
        /// Where that animateTo started this one from.
        double from;
        /// True while the controller itself is moving the client, so the scroll the
        /// list reports back for that move is told apart from the user's.
        boolean moving;
        /// The last offset the controller moved the client to.
        double lastDriven = Double.NaN;

        Attachment(Client client, ScrollPosition position) {
            this.client = client;
            this.position = position;
        }
    }

    private final List<Attachment> attachments = new ArrayList<Attachment>();

    public ScrollController() {
    }

    // Named-parameter setters.
    public void initialScrollOffset(double v) {
        this.initialScrollOffset = v;
        this.scrollPosition.setPixels(v);
    }

    public void keepScrollOffset(boolean v) {
        this.keepScrollOffset = v;
    }

    public void debugLabel(String v) {
        this.debugLabel = v;
    }

    /// The offset of the one attached scrollable. As in Flutter, which reads
    /// `positions.single`, it is an error with more than one attached: there is no
    /// single answer, and picking one silently is how a bug hides.
    public double offset() {
        return position().pixels();
    }

    /// The position of the one attached scrollable; see {@link #offset()}. With none
    /// attached it is the controller's own, which Flutter would assert on; answering
    /// keeps a read before the first layout harmless.
    public ScrollPosition position() {
        int n = attachments.size();
        if (n == 0) {
            return scrollPosition;
        }
        if (n > 1) {
            throw new dart.core.StateError("ScrollController attached to multiple scroll views.");
        }
        return attachments.get(0).position;
    }

    public boolean hasClients() {
        return attached || !attachments.isEmpty();
    }

    /** The animateTo in progress, or null. */
    private com.codename1.flutter.animation.AnimationController motion;
    private dart.async.Completer<Object> motionDone;

    /**
     * Scrolls every attached list to {@code offset} over {@code duration}, following
     * {@code curve}, and completes when they arrive -- or when the motion is interrupted
     * by a jump, another animateTo, dispose, or the user scrolling the lists themselves,
     * as Flutter's does. It used to assign the target at once and return a completed
     * future, so the list jumped and a caller awaiting the scroll resumed before any
     * motion could have run.
     */
    public Future<Object> animateTo(final double offset, Duration duration, final Curve curve) {
        interruptMotion();
        if (attachments.isEmpty() || duration == null || duration.inMilliseconds() <= 0) {
            moveAll(offset);
            return Future.value(null);
        }
        for (int i = 0; i < attachments.size(); i++) {
            Attachment a = attachments.get(i);
            a.from = a.position.pixels();
            a.driven = true;
        }
        final com.codename1.flutter.animation.AnimationController run =
                new com.codename1.flutter.animation.AnimationController();
        run.duration(duration);
        run.addListener(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                if (motion != run) {
                    return;
                }
                double v = run.value();
                double t = curve == null ? v : curve.transform(v);
                for (int i = 0; i < attachments.size(); i++) {
                    Attachment a = attachments.get(i);
                    if (a.driven) {
                        moveOne(a, a.from + (offset - a.from) * t);
                    }
                }
                notifyListeners();
            }
        });
        final dart.async.Completer<Object> done = new dart.async.Completer<Object>();
        motion = run;
        motionDone = done;
        run.forward().then(new Funcs.Func1<Object, Object>() {
            @Override
            public Object call(Object ignored) {
                if (motion == run) {
                    for (int i = 0; i < attachments.size(); i++) {
                        Attachment a = attachments.get(i);
                        if (a.driven) {
                            moveOne(a, offset);
                        }
                    }
                    notifyListeners();
                    interruptMotion();
                }
                return null;
            }
        });
        return done.future();
    }

    /** Ends an animateTo in progress where it is, completing its future. */
    private void interruptMotion() {
        com.codename1.flutter.animation.AnimationController run = motion;
        dart.async.Completer<Object> done = motionDone;
        motion = null;
        motionDone = null;
        for (int i = 0; i < attachments.size(); i++) {
            attachments.get(i).driven = false;
        }
        if (run != null) {
            run.dispose();
        }
        if (done != null) {
            done.complete(null);
        }
    }

    /// Moves one attached list, marking the move as the controller's own.
    private static void moveOne(Attachment a, double px) {
        a.position.jumpTo(px);
        drive(a, px);
    }

    /// Tells the list to move, leaving its position to what the list reports back.
    private static void drive(Attachment a, double px) {
        a.lastDriven = px;
        a.moving = true;
        try {
            a.client.scrollToOffset(px);
        } finally {
            a.moving = false;
        }
    }

    /// Moves every attached list (or, with none, the controller's own position) and
    /// notifies once.
    private void moveAll(double px) {
        if (attachments.isEmpty()) {
            scrollPosition.jumpTo(px);
        }
        for (int i = 0; i < attachments.size(); i++) {
            moveOne(attachments.get(i), px);
        }
        notifyListeners();
    }

    public void jumpTo(double value) {
        interruptMotion();
        moveAll(value);
    }

    public void addListener(Funcs.VoidFunc0 listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Funcs.VoidFunc0 listener) {
        listeners.remove(listener);
    }

    public void dispose() {
        interruptMotion();
        listeners.clear();
        attachments.clear();
        attached = false;
    }

    // ------------------------------------------------------------------
    // Framework plumbing
    // ------------------------------------------------------------------

    /**
     * The scrollable a controller drives: the mounted list moves itself to an offset.
     *
     * <p>A ListView used to store its controller and never attach it, so hasClients
     * stayed false, user scrolling never reached offset or the listeners, and
     * jumpTo/animateTo moved only this detached model while the list sat still.</p>
     */
    interface Client {
        /** Scrolls to {@code offset} logical pixels. */
        void scrollToOffset(double offset);
    }

    void attach() {
        this.attached = true;
    }

    /** Attaches a scrollable this controller drives, alongside any already attached. */
    void attach(Client c) {
        if (c == null || find(c) != null) {
            return;
        }
        // The first one takes over the controller's own position: the offset it already
        // holds -- its initialScrollOffset, or where an earlier list left it -- is where
        // the newly attached one starts. Another one gets a position of its own at the
        // initial offset, as each of Flutter's positions does.
        ScrollPosition p;
        if (attachments.isEmpty()) {
            p = scrollPosition;
        } else {
            p = new ScrollPosition();
            p.setPixels(initialScrollOffset);
        }
        Attachment a = new Attachment(c, p);
        attachments.add(a);
        if (p.pixels() != 0) {
            // Not moveOne: before the list has reported its extents the position would
            // clamp the offset to zero.
            drive(a, p.pixels());
        }
    }

    void detach() {
        this.attached = false;
        interruptMotion();
        attachments.clear();
    }

    /** Detaches {@code c}, if it is attached. */
    void detach(Client c) {
        Attachment a = find(c);
        if (a == null) {
            return;
        }
        attachments.remove(a);
        if (a.driven) {
            a.driven = false;
            stopIfNothingDriven();
        }
        if (attachments.isEmpty() && a.position != scrollPosition) {
            // The controller's own position keeps where the LAST list was, so the next
            // list to attach starts there.
            scrollPosition.setPixels(a.position.pixels());
        }
    }

    private Attachment find(Client c) {
        for (int i = 0; i < attachments.size(); i++) {
            if (attachments.get(i).client == c) {
                return attachments.get(i);
            }
        }
        return null;
    }

    /// Ends the animateTo once it drives nothing -- every list it was moving has been
    /// taken over by the user or detached -- so its future completes, as Flutter's
    /// does once the last of its scroll activities is disposed.
    private void stopIfNothingDriven() {
        if (motion == null) {
            return;
        }
        for (int i = 0; i < attachments.size(); i++) {
            if (attachments.get(i).driven) {
                return;
            }
        }
        interruptMotion();
    }

    /** The first attached list (or, with none, the controller) scrolled to {@code offset}. */
    void userScrolled(double offset, double maxExtent, double viewport) {
        if (attachments.isEmpty()) {
            scrollPosition.applyViewportDimension(viewport);
            scrollPosition.applyContentDimensions(0, maxExtent);
            if (scrollPosition.pixels() != offset) {
                scrollPosition.setPixels(offset);
                notifyListeners();
            }
            return;
        }
        userScrolled(attachments.get(0).client, offset, maxExtent, viewport);
    }

    /**
     * The attached list {@code c} reports it is at {@code offset} logical pixels.
     *
     * <p>That is either the echo of a move the controller made -- the list reports every
     * scroll, including the ones it was told to do -- or the user's own scroll. Only the
     * second interrupts an animateTo, as a drag does in Flutter; the first must not, or
     * the animation would stop itself on its first frame. A report made while the
     * controller is moving the list, or one that lands where it put it (within a pixel
     * of rounding), is the echo.</p>
     */
    void userScrolled(Client c, double offset, double maxExtent, double viewport) {
        Attachment a = find(c);
        if (a == null) {
            return;
        }
        ScrollPosition p = a.position;
        p.applyViewportDimension(viewport);
        p.applyContentDimensions(0, maxExtent);
        if (a.moving) {
            p.setPixels(offset);   // the controller notifies once it has moved them all
            return;
        }
        if (a.driven && !(Math.abs(offset - a.lastDriven) <= 1.0)) {
            a.driven = false;
            stopIfNothingDriven();
        }
        if (p.pixels() != offset) {
            p.setPixels(offset);
            notifyListeners();
        }
    }

    void notifyListeners() {
        com.codename1.flutter.foundation.Listeners.notify(listeners);
    }
}
