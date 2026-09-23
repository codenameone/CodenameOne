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

import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.accessibility.AccessibilityAction;
import com.codename1.ui.accessibility.AccessibilityRole;

import dart.runtime.Funcs;

/**
 * Leaf render box for {@link GestureOverlay}: a transparent CN1 component
 * (UIID "FlutterGesture", paints nothing, grabs pointer events) sized by the
 * parent {@link GestureRenderElement} to exactly the child's bounds. A tap
 * is a pointer release inside the bounds that neither activated a drag nor
 * followed a long press; long presses ride CN1's built-in long-press
 * dispatch.
 */
public class GestureOverlayRenderElement extends RenderElement {

    public GestureOverlayRenderElement(GestureOverlay widget) {
        super(widget);
    }

    private GestureDetector gesture() {
        Element p = parent();
        if (p instanceof GestureRenderElement) {
            return ((GestureRenderElement) p).gesture();
        }
        return null;
    }

    /**
     * Runs a gesture callback, and REPORTS anything it throws.
     *
     * <p>An exception here travelled up into Codename One's pointer dispatch, which
     * catches it, so the gesture did nothing and said nothing. A control that is visibly
     * pressed and then simply does not act is the hardest kind of defect to find from a
     * screenshot, and the sweep photographs screens at rest so it never sees one at all.
     * Whatever the handler does wrong, the error channel should carry it.</p>
     */
    private void fire(Funcs.VoidFunc0 f) {
        if (f == null) {
            return;
        }
        try {
            f.call();
        } catch (Throwable t) {
            com.codename1.flutter.FlutterErrorReport.unimplemented("Gesture",
                    "a tap handler threw " + t.getClass().getName()
                    + (t.getMessage() == null ? "" : ": " + t.getMessage()));
        }
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        Component c = new OverlayComponent();
        publishSemantics(c);
        return c;
    }

    @Override
    protected void updateComponent(Component c) {
        publishSemantics(c);
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        // The parent hands us tight constraints matching the child's bounds.
        return constraints.smallest();
    }

    /**
     * Publishes the tap target to the accessibility tree.
     *
     * <p>The overlay is the only component that knows a subtree is tappable —
     * the child it covers is ordinary content — so without this a
     * GestureDetector/InkWell is invisible to screen readers and to anything
     * driving the UI through semantics. Mirrors Flutter, which gives a
     * GestureDetector with an onTap the button role and a tap action.</p>
     */
    private void publishSemantics(Component c) {
        GestureDetector g = gesture();
        if (g == null || g.getOnTap() == null) {
            return;
        }
        try {
            c.getSemantics()
                    .setRole(AccessibilityRole.BUTTON)
                    .addAction(new AccessibilityAction(AccessibilityAction.ACTIVATE, null,
                            new AccessibilityAction.Handler() {
                                @Override
                                public boolean perform(Component component, Object argument) {
                                    GestureDetector target = gesture();
                                    if (target == null || target.getOnTap() == null) {
                                        return false;
                                    }
                                    fire(target.getOnTap());
                                    return true;
                                }
                            }));
        } catch (Throwable t) {
            // semantics are best-effort; never fail a build over them
        }
    }

    /**
     * The interactive component of THIS gesture's own subtree under {@code (x, y)}, or null.
     *
     * <p>The overlay covers its whole subtree and grabs pointer events, which is what lets a
     * GestureDetector wrap ordinary content — a label, a card — and still be tappable. But
     * it also means a wrapper sitting above a BUTTON swallows that button's presses, and
     * Flutter's hit test does the opposite: the innermost target wins. The gallery hits this
     * wherever a scrollable or a tappable region contains controls, which is how the app
     * bar's overflow button came to render perfectly and do nothing at all.</p>
     *
     * <p>Walking our own subtree — rather than everything under the point — keeps a
     * neighbouring widget's components from being handed events that were never theirs.</p>
     */
    private Component interactiveTargetAt(int x, int y) {
        Element p = parent();
        if (!(p instanceof GestureRenderElement)) {
            return null;
        }
        Element content = ((GestureRenderElement) p).contentElement();
        if (content == null) {
            return null;
        }
        java.util.List<Component> hits = new java.util.ArrayList<Component>();
        collectInteractive(content, x, y, hits);
        // Last in tree order is the topmost, and the deepest — Flutter's winner.
        return hits.isEmpty() ? null : hits.get(hits.size() - 1);
    }

    private static void collectInteractive(Element e, final int x, final int y,
            final java.util.List<Component> out) {
        if (e == null) {
            return;
        }
        if (e instanceof RenderElement) {
            Component c = ((RenderElement) e).component();
            if (c != null && c.isEnabled() && c.contains(x, y) && isInteractive(c)) {
                out.add(c);
            }
        }
        e.visitChildren(new Funcs.VoidFunc1<Element>() {
            @Override
            public void call(Element child) {
                collectInteractive(child, x, y, out);
            }
        });
    }

    /** Components that act on a press of their own — the ones a wrapper must not shadow. */
    private static boolean isInteractive(Component c) {
        return c instanceof com.codename1.ui.Button
                || c instanceof com.codename1.ui.TextArea
                || c instanceof OverlayComponent
                || isScrollPane(c);
    }

    /**
     * A pane that scrolls on its own axis.
     *
     * <p>These matter because a DRAG over them is theirs, not ours. CN1 routes a drag from
     * whichever component took the press to the nearest scrollable ancestor, so an overlay
     * stretched across the page handed every horizontal drag to the page's VERTICAL scroll
     * and the inner pane never moved — a data table wide enough to need scrolling simply
     * would not.</p>
     */
    private static boolean isScrollPane(Component c) {
        if (!(c instanceof com.codename1.ui.Container)) {
            return false;
        }
        com.codename1.ui.Container container = (com.codename1.ui.Container) c;
        return container.isScrollableX() || container.isScrollableY();
    }

    class OverlayComponent extends Component implements InkFeedback.DragAware {

        @Override
        public boolean gestureBecameDrag() {
            return isDragActivated();
        }


        private boolean suppressTap;
        /** The inner component this press was handed to, if any. */
        private Component forwardTo;
        /** Where the press landed, for the slop test in {@link #pointerReleased}. */
        private int pressX;
        private int pressY;
        /// When the press landed, so a drag can report how fast it was going.
        private long pressAt;

        OverlayComponent() {
            setUIID("FlutterGesture");
            setGrabsPointerEvents(true);
            setFocusable(false);
            getAllStyles().setBgTransparency(0);
            getAllStyles().setPadding(0, 0, 0, 0);
            getAllStyles().setMargin(0, 0, 0, 0);
        }

        @Override
        public void paint(Graphics g) {
            ink.paint(g, this);
        }

        @Override
        public void pointerPressed(int x, int y) {
            suppressTap = false;
            pressX = x;
            pressY = y;
            pressAt = com.codename1.ui.animations.AnimationTime.now();
            forwardTo = interactiveTargetAt(x, y);
            if (forwardTo != null) {
                // The press belongs to something inside us. We stay CN1's event target, so
                // an ancestor scroll still sees the drag, but the tap itself is not ours.
                forwardTo.pointerPressed(x, y);
                super.pointerPressed(x, y);
                return;
            }
            ink.press(this, x - getAbsoluteX(), y - getAbsoluteY(), inkResponse());
            super.pointerPressed(x, y);
        }

        /** The drag this detector claimed for the current press: one of the DRAG_ constants. */
        private int dragAxis = DRAG_NONE;
        private int lastX;
        private int lastY;

        @Override
        public void pointerDragged(int x, int y) {
            // A drag this detector has callbacks for is ITS drag, as in Flutter's gesture
            // arena where the innermost recognizer wins: once claimed, every move goes to
            // those callbacks and no enclosing scrollable moves. An inner scrollable still
            // takes precedence. These callbacks used to be stored and never read, so a
            // carousel, a slider or a panning surface rendered and never moved.
            GestureDetector g = gesture();
            if (dragAxis == DRAG_NONE && g != null && movedBeyondSlop(x, y)
                    && !(forwardTo != null && isScrollPane(forwardTo))) {
                dragAxis = claimAxis(x - pressX, y - pressY, g.handlesVerticalDrag(),
                        g.handlesHorizontalDrag(), g.handlesPan());
                if (dragAxis != DRAG_NONE) {
                    // Whatever was pressed inside us is not getting a tap now.
                    dragInitiated();
                    fireDragStart(g);
                    lastX = pressX;
                    lastY = pressY;
                }
            }
            if (dragAxis != DRAG_NONE) {
                fireDragUpdate(g, x, y);
                lastX = x;
                lastY = y;
                return;
            }
            // Past the slop this is a scroll, not a tap, and Flutter drops the splash --
            // whether or not Codename One has decided to call it a drag yet. Waiting for
            // its verdict leaves a highlight standing on a row the finger has left.
            if (movedBeyondSlop(x, y)) {
                ink.cancel(this);
                if (forwardTo instanceof OverlayComponent) {
                    ((OverlayComponent) forwardTo).ownInk().cancel(forwardTo);
                }
            }
            // Only a scrollable target gets the drag: handing one to a button would start a
            // press it never finishes, and CN1 already treats our own drag as a scroll.
            if (forwardTo != null && isScrollPane(forwardTo)) {
                forwardTo.pointerDragged(x, y);
                return;
            }
            super.pointerDragged(x, y);
        }

        /** This overlay's ink, so a forwarding neighbour can cancel it. */
        InkFeedback ownInk() {
            return ink;
        }

        /// Whether the pointer travelled far enough for this to be a scroll rather than
        /// a tap. See {@link #TOUCH_SLOP_LP}.
        private boolean movedBeyondSlop(int x, int y) {
            double dx = x - pressX;
            double dy = y - pressY;
            double slop = com.codename1.flutter.rendering.Dp.px(TOUCH_SLOP_LP);
            return dx * dx + dy * dy > slop * slop;
        }

        @Override
        public void dragInitiated() {
            // A drag means the press was a scroll, not a tap: Flutter cancels the splash.
            super.dragInitiated();
            ink.cancel(this);
            // And whatever we handed the press to. It is not Codename One's event target,
            // so nothing else will ever tell it the gesture ended -- its ink would stay
            // HELD, and a held press deliberately keeps its highlight standing. That is
            // why a mail row in the study went grey when touched and never came back:
            // two nested InkWells, the outer forwarding to the inner, and the inner never
            // hearing that the finger had moved away.
            Component target = forwardTo;
            forwardTo = null;
            if (target instanceof OverlayComponent) {
                ((OverlayComponent) target).dragInitiated();
            }
        }

        @Override
        public void longPointerPress(int x, int y) {
            super.longPointerPress(x, y);
            GestureDetector g = gesture();
            if (g != null && g.getOnLongPress() != null) {
                suppressTap = true;
                fire(g.getOnLongPress());
            }
        }

        @Override
        public void pointerReleased(int x, int y) {
            boolean wasDrag = movedBeyondSlop(x, y);
            if (forwardTo != null) {
                Component target = forwardTo;
                forwardTo = null;
                super.pointerReleased(x, y);
                // ALWAYS, even when the gesture turned out to be a drag. The target was
                // told the pointer went down; a press with no matching release leaves it
                // held -- a mail row in the study stayed grey after being touched and
                // never came back. A target that is itself one of these works out that it
                // was a drag from its own press point and cancels its ink instead of
                // firing, which is what Flutter does with a splash a scroll interrupted.
                target.pointerReleased(x, y);
                suppressTap = false;
                return;
            }
            super.pointerReleased(x, y);
            if (wasDrag) {
                ink.cancel(this);
            } else {
                ink.release(this);
            }
            if (!wasDrag && !suppressTap && contains(x, y)) {
                GestureDetector g = gesture();
                if (g != null) {
                    fire(g.getOnTap());
                }
            } else if (dragAxis != DRAG_NONE) {
                fireDragEnd(x, y);
            }
            dragAxis = DRAG_NONE;
            suppressTap = false;
        }

        private double lp(double px) {
            double scale = com.codename1.flutter.rendering.Dp.scale();
            return scale > 0 ? px / scale : px;
        }

        private com.codename1.flutter.Offset global(int x, int y) {
            return new com.codename1.flutter.Offset(lp(x), lp(y));
        }

        private com.codename1.flutter.Offset local(int x, int y) {
            return new com.codename1.flutter.Offset(lp(x - getAbsoluteX()), lp(y - getAbsoluteY()));
        }

        private void fireDragStart(GestureDetector g) {
            com.codename1.flutter.gestures.GestureDragStartCallback cb =
                    dragAxis == DRAG_VERTICAL ? g.getOnVerticalDragStart()
                    : dragAxis == DRAG_HORIZONTAL ? g.getOnHorizontalDragStart() : g.getOnPanStart();
            if (cb == null) {
                return;
            }
            try {
                cb.call(new com.codename1.flutter.gestures.DragStartDetails(global(pressX, pressY),
                        local(pressX, pressY)));
            } catch (Throwable t) {
                com.codename1.flutter.FlutterErrorReport.record(t);
            }
        }

        private void fireDragUpdate(GestureDetector g, int x, int y) {
            if (g == null) {
                return;
            }
            com.codename1.flutter.gestures.GestureDragUpdateCallback cb =
                    dragAxis == DRAG_VERTICAL ? g.getOnVerticalDragUpdate()
                    : dragAxis == DRAG_HORIZONTAL ? g.getOnHorizontalDragUpdate() : g.getOnPanUpdate();
            if (cb == null) {
                return;
            }
            double dx = lp(x - lastX);
            double dy = lp(y - lastY);
            com.codename1.flutter.Offset delta = dragAxis == DRAG_VERTICAL ? new com.codename1.flutter.Offset(0.0, dy)
                    : dragAxis == DRAG_HORIZONTAL ? new com.codename1.flutter.Offset(dx, 0.0)
                    : new com.codename1.flutter.Offset(dx, dy);
            Double primary = dragAxis == DRAG_VERTICAL ? Double.valueOf(dy)
                    : dragAxis == DRAG_HORIZONTAL ? Double.valueOf(dx) : null;
            try {
                cb.call(new com.codename1.flutter.gestures.DragUpdateDetails(global(x, y), local(x, y),
                        delta, primary));
            } catch (Throwable t) {
                com.codename1.flutter.FlutterErrorReport.record(t);
            }
        }

        /**
         * Reports the end of the claimed drag, with the speed it finished at.
         *
         * <p>Velocity is measured over the WHOLE press rather than the last few moves.
         * That understates a flick that began slowly, which is the safe direction to be
         * wrong in: it misses a gesture rather than inventing one, and every caller here
         * compares against a threshold. (The gallery's splash screen is entered with a
         * downward flick and left with an upward one.)</p>
         */
        private void fireDragEnd(int x, int y) {
            GestureDetector g = gesture();
            if (g == null) {
                return;
            }
            com.codename1.flutter.gestures.GestureDragEndCallback cb =
                    dragAxis == DRAG_VERTICAL ? g.getOnVerticalDragEnd()
                    : dragAxis == DRAG_HORIZONTAL ? g.getOnHorizontalDragEnd() : g.getOnPanEnd();
            if (cb == null) {
                return;
            }
            long ms = com.codename1.ui.animations.AnimationTime.now() - pressAt;
            if (ms <= 0) {
                ms = 1;
            }
            double vx = dragAxis == DRAG_VERTICAL ? 0 : lp(x - pressX) * 1000.0 / ms;
            double vy = dragAxis == DRAG_HORIZONTAL ? 0 : lp(y - pressY) * 1000.0 / ms;
            Double primary = dragAxis == DRAG_VERTICAL ? Double.valueOf(vy)
                    : dragAxis == DRAG_HORIZONTAL ? Double.valueOf(vx) : null;
            try {
                cb.call(new com.codename1.flutter.gestures.DragEndDetails(
                        new com.codename1.flutter.gestures.Velocity(new com.codename1.flutter.Offset(vx, vy)),
                        primary));
            } catch (Throwable t) {
                com.codename1.flutter.FlutterErrorReport.record(t);
            }
        }
    }

    static final int DRAG_NONE = 0;
    static final int DRAG_VERTICAL = 1;
    static final int DRAG_HORIZONTAL = 2;
    static final int DRAG_PAN = 3;

    /**
     * Which drag a detector claims once the pointer has passed the slop: vertical when
     * it has vertical callbacks and the motion is mostly vertical, horizontal likewise,
     * otherwise a pan if it has pan callbacks -- and none, leaving the motion to an
     * enclosing scrollable, when it has no callback for this direction.
     */
    static int claimAxis(double dx, double dy, boolean vertical, boolean horizontal, boolean pan) {
        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        if (vertical && ay >= ax) {
            return DRAG_VERTICAL;
        }
        if (horizontal && ax > ay) {
            return DRAG_HORIZONTAL;
        }
        return pan ? DRAG_PAN : DRAG_NONE;
    }

    /// Flutter's {@code kTouchSlop}: how far a pointer may travel and still be a tap.
    ///
    /// Codename One reports a drag as soon as it sees pointer movement, and taking that
    /// as "not a tap" cancels a tap on any tremor -- a finger on glass always moves a
    /// pixel or two, so inside a scrollable a control could be pressed, show its ink, and
    /// then do nothing. Flutter measures the DISTANCE instead and only stops calling it a
    /// tap past 18 logical pixels, which is far short of any real scroll.
    private static final double TOUCH_SLOP_LP = 18;

    /** The InkWell/InkResponse configuration for this tap area, or null for a plain gesture. */
    private com.codename1.flutter.material.InkResponse inkResponse() {
        GestureDetector g = gesture();
        return g instanceof com.codename1.flutter.material.InkResponse
                ? (com.codename1.flutter.material.InkResponse) g : null;
    }

    /**
     * The Material ink for this tap area: a splash expanding from the touch point plus the
     * press highlight underneath it.
     *
     * <p>Kept on the ELEMENT rather than the component so it survives the component being
     * re-styled or re-configured, and so a subtree rebuild mid-press cannot strand a
     * running animation.</p>
     */
    private final InkFeedback ink = new InkFeedback();
}
