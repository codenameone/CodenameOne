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
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.rendering.ScrollRootLayout;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;

import dart.runtime.Funcs;

/**
 * Base render element for the vertical scrollables
 * (SingleChildScrollView/ListView/GridView). The content subtree becomes a
 * REAL CN1 scroll boundary: this element owns a nested scrollable-Y
 * {@link Container} with its own {@link RenderHost} and
 * {@link ScrollRootLayout} scope, so the content's leaf components are flat
 * children of the pane (not of the outer host container) and CN1's native
 * tensile scrolling drives the scroll. Inside the pane the content is laid
 * out with a tight viewport width and an unbounded main axis.
 *
 * <p>Headless (no Display) there is no pane; layout runs the same
 * content-constraint math directly so scroll layout is unit-testable.</p>
 */
public abstract class ScrollRenderElement extends RenderElement {

    private Element content;
    private RenderHost innerHost;

    protected ScrollRenderElement(Widget widget) {
        super(widget);
    }

    /**
     * The widget describing the scrolled content (rebuilt on every sync from
     * the current configuration), or null for an empty scrollable.
     */
    protected abstract Widget buildContent();

    /**
     * The content, padded the way Flutter pads a scroll view that was given no
     * padding of its own.
     *
     * <p>{@code BoxScrollView} does not simply leave the padding null: it takes
     * the ambient MediaQuery padding along its OWN axis, applies that, and
     * removes it for everything inside so it is not counted twice. That is how a
     * full-screen list keeps its first item out from under the display cutout
     * without anyone writing a SafeArea, and the gallery's home list relies on
     * it entirely -- it has no padding, no SafeArea and no app bar, and without
     * this its title sat 133 device pixels too high, under the island.</p>
     *
     * <p>A list that DOES name its own padding keeps it, and a list under an app
     * bar sees nothing to add, because the Scaffold has already taken the top
     * padding off its body.</p>
     */
    protected final Widget padForScrollAxis(Widget content,
            com.codename1.flutter.EdgeInsets explicit) {
        if (explicit != null) {
            return padded(explicit, content);
        }
        if (content == null) {
            return null;
        }
        com.codename1.flutter.EdgeInsets media;
        try {
            media = com.codename1.flutter.MediaQuery.paddingOf(this);
        } catch (Throwable noMediaQuery) {
            return content;
        }
        if (media == null) {
            return content;
        }
        boolean across = horizontal();
        double left = across ? media.left() : 0;
        double right = across ? media.right() : 0;
        double top = across ? 0 : media.top();
        double bottom = across ? 0 : media.bottom();
        if (left == 0 && right == 0 && top == 0 && bottom == 0) {
            return content;
        }
        Widget inner = com.codename1.flutter.MediaQuery.removePadding(this,
                across ? Boolean.TRUE : Boolean.FALSE,
                across ? Boolean.FALSE : Boolean.TRUE,
                across ? Boolean.TRUE : Boolean.FALSE,
                across ? Boolean.FALSE : Boolean.TRUE,
                content);
        return padded(com.codename1.flutter.EdgeInsets.only(left, top, right, bottom), inner);
    }

    private static Widget padded(com.codename1.flutter.EdgeInsets insets, Widget child) {
        Padding p = new Padding();
        p.padding(insets);
        p.child(child);
        return p;
    }

    /**
     * When true the scrollable sizes its main axis to the content instead of
     * filling the incoming constraints.
     */
    protected boolean shrinkWrap() {
        return false;
    }

    /**
     * The scroll axis. Horizontal scrollables lay their content out with a
     * tight viewport height and an unbounded width — the mirror of the
     * vertical contract — and hand CN1 an X-scrollable pane.
     */
    protected boolean horizontal() {
        return false;
    }

    /**
     * Whether CN1's scroll indicator is suppressed.
     *
     * <p>Flutter shows a scrollbar only where the tree asks for one, by wrapping the
     * scrollable in a {@link Scrollbar} or {@link RawScrollbar}; a bare ListView,
     * SingleChildScrollView or PageView draws none. Codename One draws one by default,
     * so without this every scrollable in a transpiled app carried a bar Flutter never
     * put there — on the gallery's carousel it painted a black thumb across the bottom
     * edge of the study card.</p>
     *
     * <p>An ancestor walk is the right test because that is exactly the relationship
     * Flutter uses: {@code Scrollbar} WRAPS the scrollable it decorates.</p>
     */
    /**
     * Whether this pane draws no scrollbar.
     *
     * <p>A Flutter {@code Scrollbar} is INVISIBLE at rest — the thumb fades in
     * while the list is moving and fades out again — unless the app asks for
     * {@code thumbVisibility: true}. Codename One's is always on, so wrapping a
     * list in a Scrollbar used to paint a permanent bar down the edge of a
     * screen that should have none.</p>
     */
    protected boolean hideScrollbar() {
        for (Element a = parent(); a != null; a = a.parent()) {
            Widget w = a.widget();
            if (w instanceof Scrollbar) {
                return !((Scrollbar) w).isThumbVisible();
            }
            if (w instanceof RawScrollbar) {
                return !((RawScrollbar) w).isThumbVisible();
            }
        }
        return true;
    }

    /// The direction last reported to the widgets above this scroll view.
    private com.codename1.flutter.rendering.ScrollDirection reportedDirection =
            com.codename1.flutter.rendering.ScrollDirection.idle;

    /**
     * Raises a {@code UserScrollNotification} when the drag turns around.
     *
     * <p>Flutter raises one when the user starts or stops dragging rather than per pixel,
     * so reporting only a CHANGE of direction is both the right shape and the reason this
     * is cheap: a listener that moves chrome runs once per turn, not once per frame.</p>
     *
     * <p>Moving further into the content is {@code reverse} -- the direction the content
     * travels, not the finger. That is the sense Reply reads: reverse folds the bar away,
     * forward brings it back.</p>
     */
    private void noteScroll(int now, int before) {
        // Only while the finger is actually down. Flutter raises this from the DRAG, so
        // momentum, the settle at the end of a fling and the bounce at an edge never
        // speak -- and here they were the loudest thing in the room. Reading down the
        // list overshot to 666 and settled back to 609, so the drag ENDED by reporting
        // the direction opposite to the one the user made, and dragging back up bounced
        // past zero and reported the other one. The bar folded away when you scrolled
        // back to the top and reappeared as you read on: the right animation, driven
        // backwards, which is worse than none.
        Component c = component();
        if (c instanceof ScrollPane && !((ScrollPane) c).userDragging()) {
            // Between drags there is no user direction. Clearing it means the next drag
            // reports its first move rather than being swallowed as "no change".
            reportedDirection = com.codename1.flutter.rendering.ScrollDirection.idle;
            return;
        }
        if (now == before) {
            return;
        }
        com.codename1.flutter.rendering.ScrollDirection d = now > before
                ? com.codename1.flutter.rendering.ScrollDirection.reverse
                : com.codename1.flutter.rendering.ScrollDirection.forward;
        if (d == reportedDirection) {
            return;
        }
        reportedDirection = d;
        UserScrollNotification n = new UserScrollNotification();
        n.direction(d);
        n.dispatch(this);
    }

    private RenderHost innerHost() {
        if (innerHost == null) {
            innerHost = new RenderHost();
            innerHost.rootSupplier(new Funcs.Func0<Element>() {
                @Override
                public Element call() {
                    return content;
                }
            });
        }
        return innerHost;
    }

    @Override
    protected RenderHost hostForChild(int slot) {
        return innerHost();
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        Container pane = createPane(horizontal()
                ? new com.codename1.flutter.rendering.HorizontalScrollRootLayout(innerHost())
                : new ScrollRootLayout(innerHost()));
        pane.setUIID("FlutterScroll");
        pane.getAllStyles().setPadding(0, 0, 0, 0);
        pane.getAllStyles().setMargin(0, 0, 0, 0);
        pane.getAllStyles().setBgTransparency(0);
        if (horizontal()) {
            pane.setScrollableX(true);
            pane.setScrollableY(false);
        } else {
            pane.setScrollableY(true);
        }
        if (hideScrollbar()) {
            pane.setScrollVisible(false);
        }
        innerHost().container(pane);
        // Flutter reports a drag's direction to the widgets above the scroll view, and
        // apps steer real chrome with it: Reply folds its bottom bar away while you read
        // down a list and brings it back when you turn around. Nothing ever raised one of
        // these, so every NotificationListener in the gallery was inert.
        pane.addScrollListener(new com.codename1.ui.events.ScrollListener() {
            @Override
            public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
                noteScroll(horizontal() ? scrollX : scrollY,
                        horizontal() ? oldscrollX : oldscrollY);
            }
        });
        return pane;
    }

    /** The scrolling pane itself, so a subclass can add behaviour such as page snapping. */
    protected Container createPane(com.codename1.ui.layouts.Layout layout) {
        return new ScrollPane(layout);
    }

    /**
     * The scrolling container, with one thing added: whether the user's finger is on it.
     *
     * <p>Codename One knows, but keeps {@code isDragActivated} protected, and the answer is
     * what separates a drag from everything else the scroll offset does by itself.</p>
     */
    public static class ScrollPane extends Container {

        public ScrollPane(com.codename1.ui.layouts.Layout layout) {
            super(layout);
        }

        /** Whether this pane is currently being dragged by the user. */
        public boolean userDragging() {
            return isDragActivated();
        }

        /** A position asked for before the pane had a size, applied at its first layout. */
        private int pendingPx = -1;
        private boolean pendingHorizontal;

        /**
         * Moves the scroll position -- what a ScrollController's jumpTo drives. Before
         * the pane has been laid out there is nothing to scroll (and the position may
         * be clamped to zero), so it is kept and applied once layout gives the pane a
         * size: a controller's initialScrollOffset reaches the list that way.
         */
        public void scrollToPosition(int px, boolean horizontal) {
            if ((horizontal ? getWidth() : getHeight()) <= 0) {
                pendingPx = px;
                pendingHorizontal = horizontal;
                return;
            }
            pendingPx = -1;
            if (horizontal) {
                setScrollX(px);
            } else {
                setScrollY(px);
            }
        }

        @Override
        public void layoutContainer() {
            super.layoutContainer();
            if (pendingPx >= 0 && (pendingHorizontal ? getWidth() : getHeight()) > 0) {
                int px = pendingPx;
                pendingPx = -1;
                if (pendingHorizontal) {
                    setScrollX(px);
                } else {
                    setScrollY(px);
                }
            }
        }
    }

    @Override
    protected void syncChildren() {
        content = updateChild(content, buildContent(), 0);
    }

    @Override
    public void visitChildren(Funcs.VoidFunc1<Element> visitor) {
        if (content != null) {
            visitor.call(content);
        }
    }

    public Element contentElement() {
        return content;
    }

    protected RenderElement contentRender() {
        return findRenderElement(content);
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        return horizontal() ? layoutHorizontal(constraints) : layoutVertical(constraints);
    }

    private Size layoutVertical(BoxConstraints constraints) {
        RenderElement c = contentRender();
        double width = constraints.hasBoundedWidth() ? constraints.maxWidth() : 0;
        viewport(width, constraints.hasBoundedHeight() ? constraints.maxHeight() : 0);
        Size cs = Size.ZERO;
        if (c != null) {
            cs = c.layout(constraints.hasBoundedWidth()
                    ? ScrollRootLayout.contentConstraints(width)
                    : BoxConstraints.loose(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
            if (!constraints.hasBoundedWidth()) {
                width = cs.width();
            }
        }
        double height;
        // A SingleChildScrollView takes its CHILD's height, clamped to what it
        // was offered -- Flutter constrains the viewport to the child's size
        // rather than filling. Filling is right for a ListView and wrong here:
        // the progress demo is Center(SingleChildScrollView(...)), and a
        // viewport that fills leaves the Center nothing to centre, so the two
        // indicators sat at the top of the page against the reference's middle.
        if (shrinkWrap() || isSingleChild() || !constraints.hasBoundedHeight()) {
            height = cs.height();
        } else {
            height = constraints.maxHeight();
        }
        return constraints.constrain(new Size(width, height));
    }

    /**
     * The viewport this scrollable presents, reported BEFORE the content is
     * laid out. Content that needs to size itself against the viewport (a
     * PageView's pages take a fraction of it) cannot read {@code size()} — that
     * is only assigned after this layout returns, so it would see a stale or
     * zero extent.
     */
    protected void viewport(double width, double height) {
    }

    /** The vertical contract with the axes swapped. */
    private Size layoutHorizontal(BoxConstraints constraints) {
        RenderElement c = contentRender();
        double height = constraints.hasBoundedHeight() ? constraints.maxHeight() : 0;
        viewport(constraints.hasBoundedWidth() ? constraints.maxWidth() : 0, height);
        Size cs = Size.ZERO;
        if (c != null) {
            cs = c.layout(constraints.hasBoundedHeight()
                    ? com.codename1.flutter.rendering.HorizontalScrollRootLayout.contentConstraints(height)
                    : BoxConstraints.loose(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
            if (!constraints.hasBoundedHeight()) {
                height = cs.height();
            }
        }
        double width;
        if (shrinkWrap() || !constraints.hasBoundedWidth()) {
            width = cs.width();
        } else {
            width = constraints.maxWidth();
        }
        return constraints.constrain(new Size(width, height));
    }

    @Override
    protected void positionChildren(int x, int y) {
        // With a real pane the content lives in the inner host and the pane's
        // ScrollRootLayout positions it in pane coordinates. Headless we
        // position the content directly so tests observe absolute positions.
        if (component() == null) {
            RenderElement c = contentRender();
            if (c != null) {
                c.position(x, y);
            }
        }
    }

    /** Whether this is a SingleChildScrollView rather than a list viewport. */
    private boolean isSingleChild() {
        return widget() instanceof SingleChildScrollView;
    }
}
