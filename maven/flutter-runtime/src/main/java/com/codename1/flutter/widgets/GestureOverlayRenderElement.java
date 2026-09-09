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

    class OverlayComponent extends Component {

        private boolean suppressTap;
        /** The inner component this press was handed to, if any. */
        private Component forwardTo;

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

        @Override
        public void pointerDragged(int x, int y) {
            // Only a scrollable target gets the drag: handing one to a button would start a
            // press it never finishes, and CN1 already treats our own drag as a scroll.
            if (forwardTo != null && isScrollPane(forwardTo)) {
                forwardTo.pointerDragged(x, y);
                return;
            }
            super.pointerDragged(x, y);
        }

        @Override
        public void dragInitiated() {
            // A drag means the press was a scroll, not a tap: Flutter cancels the splash.
            super.dragInitiated();
            ink.cancel(this);
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
            boolean wasDrag = isDragActivated();
            if (forwardTo != null) {
                Component target = forwardTo;
                forwardTo = null;
                super.pointerReleased(x, y);
                // A drag was a scroll, not a tap on the control: let it go, as CN1 would.
                if (!wasDrag) {
                    target.pointerReleased(x, y);
                }
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
            }
            suppressTap = false;
        }
    }

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
