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

package com.codename1.flutter.widgets;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

/**
 * Swipe to dismiss: the child follows the finger, a background shows behind it, and past
 * a threshold it leaves and the callback fires.
 *
 * <p>The widget used to render its child and drop everything else, so a full swipe across
 * a mail row in the Reply study changed 0.0% of the screen.</p>
 *
 * <p><b>Why this listens on the FORM rather than overriding the component's pointer
 * methods.</b> The rows live in a list that scrolls vertically. Codename One decides who
 * owns a gesture before a component sees it, and a horizontal drag inside a vertical
 * scroller is claimed by the scroller -- so the row is never asked. A form pointer
 * listener runs BEFORE that decision and can consume the event, which is the same hook
 * {@code Tabs} uses to swipe between tabs inside a scrollable form. Consuming is what
 * stops the list scrolling underneath the swipe.</p>
 *
 * <p>The axis is decided once per gesture, on the first movement past the touch slop, and
 * never revisited: a swipe that starts horizontal stays a swipe even if the finger
 * wanders, and one that starts vertical is left to the list for the rest of the gesture.
 * Deciding per packet makes a diagonal drag fight itself.</p>
 */
public class DismissibleRenderElement extends RenderElement {

    /// Fraction of the width a drag must cross to dismiss, when the widget names none.
    private static final double DEFAULT_THRESHOLD = 0.4;

    /// How far a finger may travel before the gesture commits to an axis.
    private static final double SLOP_LP = 18;

    /// How long the child takes to leave, and to spring back.
    private static final int DISMISS_MS = 200;
    private static final int RESTORE_MS = 200;

    private com.codename1.flutter.Element backgroundEl;
    private com.codename1.flutter.Element secondaryEl;
    private com.codename1.flutter.Element childEl;
    private RenderElement background;
    private RenderElement secondary;
    private RenderElement child;

    private double dragX;
    /// The offset currently applied to the child's components.
    private int appliedX;
    private boolean pressedInside;
    private boolean owning;
    private boolean decided;
    private int pressX;
    private int pressY;

    private Motion slide;
    private double slideFrom;
    private double slideTo;
    private boolean dismissing;
    /// Set once the row has left: it must not be laid out again.
    private boolean gone;
    private DismissDirection dismissingTo;
    private com.codename1.ui.animations.Animation clock;

    private ActionListener<ActionEvent> pressListener;
    private ActionListener<ActionEvent> dragListener;
    private ActionListener<ActionEvent> releaseListener;
    private Form listeningOn;

    public DismissibleRenderElement(Widget widget) {
        super(widget);
    }

    private Dismissible dismissible() {
        return (Dismissible) widget();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        childEl = updateChild(childEl, dismissible().getChild(), 0);
        backgroundEl = updateChild(backgroundEl, dismissible().getBackground(), 1);
        secondaryEl = updateChild(secondaryEl, dismissible().getSecondaryBackground(), 2);
        if (gone) {
            // UNMOUNT it, rather than merely skipping its layout. A dismissed row that is
            // still mounted is still rebuilt, and it rebuilds with ancestors on their way
            // out -- its provider lookup then answers null and the build that captured it
            // fails later, inside a LayoutBuilder, far from here.
            childEl = updateChild(childEl, null, 0);
            backgroundEl = updateChild(backgroundEl, null, 1);
            secondaryEl = updateChild(secondaryEl, null, 2);
            child = null;
            background = null;
            secondary = null;
            return constraints.smallest();
        }
        child = RenderElement.findRenderElement(childEl);
        background = RenderElement.findRenderElement(backgroundEl);
        secondary = RenderElement.findRenderElement(secondaryEl);
        if (child == null) {
            return constraints.smallest();
        }
        Size size = child.layout(constraints);
        // The backgrounds fill exactly what the child occupies: they are what shows
        // through as it moves off, so anything else leaves a gap at the edge.
        BoxConstraints exact = BoxConstraints.tight(size.width(), size.height());
        if (background != null) {
            background.layout(exact);
        }
        if (secondary != null) {
            secondary.layout(exact);
        }
        return size;
    }

    /**
     * Visits the three children.
     *
     * <p>{@code Element.visitChildren} is EMPTY by default, so an element that owns
     * children and does not override it owns them invisibly: the walk that unmounts a
     * subtree never reaches them. A dismissed row was deactivated and its child,
     * background and secondary background stayed mounted with their components still in
     * the scene -- every surviving row then drew its new content over the old, an
     * orphaned subtree rebuilt with its ancestors gone and its provider lookup answered
     * null, and the inner lists inside those rows were never unmounted at all.</p>
     */
    @Override
    public void visitChildren(dart.runtime.Funcs.VoidFunc1<com.codename1.flutter.Element>
            visitor) {
        if (childEl != null) {
            visitor.call(childEl);
        }
        if (backgroundEl != null) {
            visitor.call(backgroundEl);
        }
        if (secondaryEl != null) {
            visitor.call(secondaryEl);
        }
    }

    @Override
    protected void positionChildren(int x, int y) {
        if (background != null) {
            background.position(x, y);
        }
        if (secondary != null) {
            secondary.position(x, y);
        }
        if (child != null) {
            child.position(x, y);
        }
        // Layout has just re-placed everything at rest, so whatever was applied is gone.
        appliedX = 0;
        applyDrag();
        attach();
        applyVisibility();
    }

    /// Only the background the swipe is heading towards is shown; both at rest would
    /// paint one over the other and the wrong one would win.
    private void applyVisibility() {
        Component b = background == null ? null : background.component();
        Component s = secondary == null ? null : secondary.component();
        if (b != null) {
            b.setVisible(dragX > 0);
        }
        if (s != null) {
            s.setVisible(dragX < 0);
        }
    }

    // ------------------------------------------------------------------
    // Gesture
    // ------------------------------------------------------------------

    /// The first real component under a render element.
    ///
    /// A wrapper owns none of its own, so asking the child directly answers null and the
    /// listeners were never attached -- the swipe did nothing because nothing was
    /// listening for it.
    private static Component firstComponentOf(RenderElement e) {
        if (e == null) {
            return null;
        }
        Component own = e.component();
        if (own != null) {
            return own;
        }
        for (RenderElement c : e.renderChildren()) {
            Component found = firstComponentOf(c);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /// The outermost components of a subtree.
    ///
    /// Only these may be moved: a Codename One component's x is relative to its parent,
    /// so shifting a nested one as well would move it twice.
    private static void topComponents(RenderElement e, java.util.List<Component> out) {
        if (e == null) {
            return;
        }
        Component c = e.component();
        if (c != null) {
            out.add(c);
            return;
        }
        for (RenderElement k : e.renderChildren()) {
            topComponents(k, out);
        }
    }

    /// Slides the child to the current drag offset, by the DIFFERENCE since last time.
    /// Re-placing it from the element's own origin does not work: a render element inside
    /// a scrolling list reports (0,0), so that put the row in the corner.
    private void applyDrag() {
        int want = (int) Math.round(dragX);
        int delta = want - appliedX;
        if (delta != 0) {
            java.util.List<Component> tops = new java.util.ArrayList<Component>();
            topComponents(child, tops);
            for (int i = 0; i < tops.size(); i++) {
                Component c = tops.get(i);
                c.setX(c.getX() + delta);
            }
            appliedX = want;
        }
    }

    private Component self() {
        return firstComponentOf(child);
    }

    /// Whether a press landed on this row, tested against the CHILD COMPONENT's absolute
    /// bounds rather than the render element's.
    ///
    /// A render element inside a scrolling list reports (0,0) for its own position -- the
    /// placement lives on the Codename One components -- so hit-testing the element put
    /// every row's rectangle at the origin and the wrong rows answered.
    /// The row's extent on screen: the union of every component under it.
    ///
    /// Neither end of the tree answers this on its own. The render element reports (0,0)
    /// for its position inside a scrolling list -- the placement lives on the Codename
    /// One components -- and the first component going down is a leaf, a label inside the
    /// card rather than the card. Hit-testing either one picked the wrong rows.
    private int[] rowBounds() {
        int[] b = new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE,
            Integer.MIN_VALUE, Integer.MIN_VALUE};
        union(child, b);
        return b[0] == Integer.MAX_VALUE ? null : b;
    }

    private static void union(RenderElement e, int[] b) {
        if (e == null) {
            return;
        }
        Component c = e.component();
        if (c != null && c.getWidth() > 0 && c.getHeight() > 0) {
            int l = c.getAbsoluteX();
            int t = c.getAbsoluteY();
            b[0] = Math.min(b[0], l);
            b[1] = Math.min(b[1], t);
            b[2] = Math.max(b[2], l + c.getWidth());
            b[3] = Math.max(b[3], t + c.getHeight());
        }
        for (RenderElement k : e.renderChildren()) {
            union(k, b);
        }
    }

    private boolean withinRow(int x, int y) {
        int[] b = rowBounds();
        return b != null && x >= b[0] && x < b[2] && y >= b[1] && y < b[3];
    }

    private void attach() {
        Component anchor = self();
        Form f = anchor == null ? null : anchor.getComponentForm();
        if (f == null || f == listeningOn) {
            return;
        }
        detach();
        listeningOn = f;
        pressListener = new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (dismissing) {
                    return;
                }
                pressedInside = withinRow(evt.getX(), evt.getY());
                owning = false;
                decided = false;
                pressX = evt.getX();
                pressY = evt.getY();
            }
        };
        dragListener = new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (!pressedInside || dismissing) {
                    return;
                }
                double dx = evt.getX() - pressX;
                double dy = evt.getY() - pressY;
                if (!decided) {
                    double slop = Dp.px(SLOP_LP);
                    if (Math.abs(dx) < slop && Math.abs(dy) < slop) {
                        return;
                    }
                    // Decided ONCE, and for the whole gesture.
                    decided = true;
                    owning = Math.abs(dx) > Math.abs(dy) && allows(dx);
                }
                if (!owning) {
                    return;
                }
                dragX = dx;
                evt.consume();
                repaintRow();
            }
        };
        releaseListener = new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (!pressedInside) {
                    return;
                }
                pressedInside = false;
                if (!owning || dismissing) {
                    return;
                }
                owning = false;
                evt.consume();
                settle();
            }
        };
        f.addPointerPressedListener(pressListener);
        f.addPointerDraggedListener(dragListener);
        f.addPointerReleasedListener(releaseListener);
    }

    private void detach() {
        if (listeningOn == null) {
            return;
        }
        listeningOn.removePointerPressedListener(pressListener);
        listeningOn.removePointerDraggedListener(dragListener);
        listeningOn.removePointerReleasedListener(releaseListener);
        listeningOn = null;
    }

    /// Whether the widget accepts a swipe going this way at all.
    private boolean allows(double dx) {
        Object d = dismissible().getDirection();
        if (d == DismissDirection.none) {
            return false;
        }
        if (d == DismissDirection.startToEnd) {
            return dx > 0;
        }
        if (d == DismissDirection.endToStart) {
            return dx < 0;
        }
        // horizontal, or unstated: Flutter's default is horizontal.
        return true;
    }

    private DismissDirection directionOf(double dx) {
        return dx < 0 ? DismissDirection.endToStart : DismissDirection.startToEnd;
    }

    /// The fraction of the width this direction must cross, from {@code dismissThresholds}.
    private double thresholdFor(DismissDirection dir) {
        Object t = dismissible().getDismissThresholds();
        if (t instanceof java.util.Map) {
            Object v = ((java.util.Map<?, ?>) t).get(dir);
            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            }
        }
        return DEFAULT_THRESHOLD;
    }

    private void settle() {
        int[] b = rowBounds();
        double width = b == null ? (size() == null ? 0 : size().width()) : (b[2] - b[0]);
        if (width <= 0) {
            animateTo(0, RESTORE_MS, false, null);
            return;
        }
        final DismissDirection dir = directionOf(dragX);
        boolean past = Math.abs(dragX) / width >= thresholdFor(dir);
        if (!past) {
            animateTo(0, RESTORE_MS, false, null);
            return;
        }
        final double target = dragX < 0 ? -width : width;
        confirm(dir, new dart.runtime.Funcs.VoidFunc1<Boolean>() {
            @Override
            public void call(Boolean ok) {
                if (ok.booleanValue()) {
                    animateTo(target, DISMISS_MS, true, dir);
                } else {
                    animateTo(0, RESTORE_MS, false, null);
                }
            }
        });
    }

    /**
     * {@code confirmDismiss}, which may veto -- and usually answers with a Future, from
     * an async callback or a dialog. That future used to count as consent, because only a
     * Boolean was looked at: an asynchronous "no", or a failed confirmation, dismissed the
     * row at once and ran onDismissed anyway. A Future now decides when it completes (an
     * error vetoes), and the row holds where it was dragged until then, as in Flutter. No
     * callback, or an answer that is neither, is consent.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void confirm(DismissDirection dir, final dart.runtime.Funcs.VoidFunc1<Boolean> decide) {
        dart.runtime.Funcs.Func1<DismissDirection, Object> c = dismissible().getConfirmDismiss();
        if (c == null) {
            decide.call(Boolean.TRUE);
            return;
        }
        Object answer;
        try {
            answer = c.call(dir);
        } catch (Throwable t) {
            com.codename1.flutter.FlutterErrorReport.record(t);
            decide.call(Boolean.FALSE);
            return;
        }
        if (answer instanceof dart.async.Future) {
            ((dart.async.Future) answer).then(new dart.runtime.Funcs.VoidFunc1<Object>() {
                @Override
                public void call(Object v) {
                    onEdt(decide, Boolean.valueOf(!(v instanceof Boolean) || ((Boolean) v).booleanValue()));
                }
            }).catchError(new dart.runtime.Funcs.VoidFunc1<Object>() {
                @Override
                public void call(Object error) {
                    com.codename1.flutter.FlutterErrorReport.record(error);
                    onEdt(decide, Boolean.FALSE);
                }
            });
            return;
        }
        decide.call(Boolean.valueOf(!(answer instanceof Boolean) || ((Boolean) answer).booleanValue()));
    }

    private static void onEdt(final dart.runtime.Funcs.VoidFunc1<Boolean> decide, final Boolean ok) {
        if (com.codename1.ui.Display.isInitialized() && !com.codename1.ui.CN.isEdt()) {
            com.codename1.ui.CN.callSerially(new Runnable() {
                @Override
                public void run() {
                    decide.call(ok);
                }
            });
        } else {
            decide.call(ok);
        }
    }

    private void animateTo(double target, int duration, boolean dismiss, DismissDirection dir) {
        slideFrom = dragX;
        slideTo = target;
        dismissing = dismiss;
        dismissingTo = dir;
        slide = Motion.createEaseInOutMotion(0, 1000, duration);
        slide.start();
        startClock();
    }

    private void startClock() {
        Component anchor = self();
        final Form f = anchor == null ? null : anchor.getComponentForm();
        if (f == null || clock != null) {
            return;
        }
        clock = new com.codename1.ui.animations.Animation() {
            @Override
            public boolean animate() {
                if (slide == null) {
                    stopClock(f);
                    return false;
                }
                double t = slide.getValue() / 1000.0;
                dragX = slideFrom + (slideTo - slideFrom) * t;
                repaintRow();
                if (slide.isFinished()) {
                    slide = null;
                    stopClock(f);
                    if (dismissing) {
                        dismissing = false;
                        fireDismissed(dismissingTo);
                    }
                    // The repaint belongs AFTER the application has removed the item, not
                    // here: the callback is deferred to the next frame, so repainting now
                    // paints the list as it still is, and the state it ends up in is never
                    // painted at all. That left the rows that moved up showing their old
                    // text under their new text.
                }
                // Repaint the ROW, never the screen: returning true here makes the whole
                // form flush every frame, which shows as a flicker behind the swipe.
                return false;
            }

            @Override
            public void paint(com.codename1.ui.Graphics g) {
            }
        };
        f.registerAnimated(clock);
    }

    private void stopClock(Form f) {
        if (clock != null && f != null) {
            f.deregisterAnimated(clock);
        }
        clock = null;
    }

    /// Tells the application the row is gone, on the NEXT frame rather than this one.
    ///
    /// The callback removes the item, which rebuilds the list -- and this runs from an
    /// animation tick, in the middle of the frame that is still laying this very row out.
    /// Removing it there left the dying subtree being laid out with its ancestors already
    /// gone, and the provider lookup inside it returned null: "Cannot invoke
    /// EmailStore.isEmailStarred because emailStore is null". Handing it to the next
    /// frame lets this one finish first.
    private void fireDismissed(final DismissDirection dir) {
        final dart.runtime.Funcs.VoidFunc1<DismissDirection> cb =
                dismissible().getOnDismissed();
        if (cb == null) {
            return;
        }
        gone = true;
        detach();
        com.codename1.ui.Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                try {
                    cb.call(dir);
                } catch (Throwable t) {
                    com.codename1.flutter.FlutterErrorReport.record(t);
                }
                // A frame later again, and a REVALIDATE rather than a repaint. Removing
                // the item schedules its own rebuild, so repainting here paints the list
                // as it still is; and the rows that move up leave their old pixels behind,
                // which only a full re-layout and repaint clears.
                com.codename1.ui.Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        com.codename1.ui.Form now = com.codename1.ui.Display.getInstance()
                                .getCurrent();
                        if (now != null) {
                            now.revalidate();
                            now.repaint();
                        }
                    }
                });
            }
        });
    }

    private void repaintRow() {
        applyDrag();
        applyVisibility();
        Component c = self();
        if (c != null) {
            c.repaint();
        }
        Component b = firstComponentOf(background);
        if (b != null) {
            b.repaint();
        }
        Component s = firstComponentOf(secondary);
        if (s != null) {
            s.repaint();
        }
    }
}
