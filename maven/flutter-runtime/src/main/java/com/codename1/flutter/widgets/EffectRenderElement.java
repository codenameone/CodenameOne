package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.FlutterRootLayout;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;

import dart.runtime.Funcs;

/**
 * Base for widgets that change how their subtree PAINTS without changing its layout —
 * Opacity, Transform and their relatives.
 *
 * <p>The rest of the render tree is flat: every element's component is a sibling in one
 * host container, positioned absolutely. That is fast, but it means an ancestor cannot
 * wrap its descendants in a paint effect, because they are not its children. So an
 * effect element owns a nested container with its own {@link RenderHost} — the same
 * device the scrollables use — which makes its subtree genuinely nested and therefore
 * something it can paint through.</p>
 *
 * <p>Layout is untouched: the child is measured against the incoming constraints and the
 * effect element takes exactly the child's size. Flutter's Opacity and Transform do not
 * affect layout either.</p>
 */
public abstract class EffectRenderElement extends RenderElement {

    private Element content;
    private RenderHost innerHost;

    protected EffectRenderElement(Widget widget) {
        super(widget);
    }

    /** The widget this effect applies to. */
    protected abstract Widget effectChild();

    /**
     * Applies the effect and paints the subtree. Implementations must leave the
     * Graphics as they found it — a frame paints many components through the same one.
     */
    protected abstract void paintWithEffect(Graphics g, Container pane, Runnable paintChildren);

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
        EffectPane pane = new EffectPane(innerHost());
        innerHost().container(pane);
        return pane;
    }

    @Override
    protected void syncChildren() {
        content = updateChild(content, effectChild(), 0);
    }

    @Override
    public void visitChildren(Funcs.VoidFunc1<Element> visitor) {
        if (content != null) {
            visitor.call(content);
        }
    }

    /// The constraints the Flutter pass last gave this effect.
    ///
    /// The nested pane carries its own {@link FlutterRootLayout}, and Codename One runs
    /// that layout independently, deriving constraints from the pane's CURRENT component
    /// size. That size is only the Flutter-assigned one after {@code position} has written
    /// it; before then it is whatever CN1 last put there — for a fresh subtree, its
    /// preferred size. So the subtree could be laid out against a height it was never
    /// given: the gallery's study card was measured at its unconstrained 272dp instead of
    /// the carousel's 240dp viewport, which is why its caption was clipped and its bottom
    /// corners came out square.
    ///
    /// Remembering the real constraints and handing them to the nested pass removes the
    /// disagreement: the Flutter pass owns this subtree's geometry, and CN1's pass must
    /// reproduce it rather than re-derive it.
    private BoxConstraints lastConstraints;

    BoxConstraints effectConstraints() {
        return lastConstraints;
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        lastConstraints = constraints;
        RenderElement c = findRenderElement(content);
        if (c == null) {
            return constraints.smallest();
        }
        Size cs = c.layout(constraints);
        c.position(0, 0);   // inside our pane, the child sits at the origin
        return constraints.constrain(cs);
    }

    /**
     * An effect is a PAINT wrapper: {@link #performLayout} takes its size straight
     * from the child, so no configuration of the effect itself - a Transform's
     * scale, a Material's colour or elevation - can move anything.
     *
     * <p>This matters most where it is animated. The gallery's carousel rebuilds a
     * Transform per card per scroll frame; treating that as a layout change marked
     * every ancestor up to the Scaffold dirty and relayed out the whole page on
     * each frame, which is what made dragging the carousel stutter.</p>
     */
    @Override
    protected boolean updateAffectsLayout() {
        return false;
    }

    @Override
    protected void positionChildren(int x, int y) {
        // The pane's own layout places the subtree in pane coordinates; nothing to do
        // here, and positioning the child again in host coordinates would double-offset it.
    }

    /** The nested container: lays the subtree out at its own bounds and paints it through the effect. */
    private final class EffectPane extends Container {

        EffectPane(RenderHost host) {
            super(new FlutterRootLayout(host) {
                @Override
                protected BoxConstraints constraintsFor(com.codename1.ui.Container parent) {
                    BoxConstraints c = effectConstraints();
                    return c != null ? c : super.constraintsFor(parent);
                }
            });
            setUIID("FlutterEffect");
            getAllStyles().setPadding(0, 0, 0, 0);
            getAllStyles().setMargin(0, 0, 0, 0);
            getAllStyles().setBgTransparency(0);
        }

        @Override
        public void paint(final Graphics g) {
            final Container self = this;
            paintWithEffect(g, self, new Runnable() {
                @Override
                public void run() {
                    EffectPane.super.paint(g);
                }
            });
        }
    }
}
