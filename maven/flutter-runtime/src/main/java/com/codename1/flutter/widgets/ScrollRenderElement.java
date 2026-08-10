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
    protected boolean hideScrollbar() {
        for (Element a = parent(); a != null; a = a.parent()) {
            Widget w = a.widget();
            if (w instanceof Scrollbar || w instanceof RawScrollbar) {
                return false;
            }
        }
        return true;
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
        return pane;
    }

    /** The scrolling pane itself, so a subclass can add behaviour such as page snapping. */
    protected Container createPane(com.codename1.ui.layouts.Layout layout) {
        return new Container(layout);
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
        if (shrinkWrap() || !constraints.hasBoundedHeight()) {
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
}
