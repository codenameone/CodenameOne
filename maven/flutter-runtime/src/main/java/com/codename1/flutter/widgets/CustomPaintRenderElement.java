package com.codename1.flutter.widgets;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.CustomPainter;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.GraphicsCanvas;
import com.codename1.flutter.rendering.Size;
import com.codename1.io.Log;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;

/**
 * Runs a {@link CustomPainter} onto the screen — the render element behind
 * {@link CustomPaint}.
 *
 * <p>The element owns a CN1 component whose {@code paint} drives
 * {@code painter.paint(canvas, size)} through a {@link GraphicsCanvas}. The
 * painter is handed a size in LOGICAL pixels and a canvas pre-scaled by the
 * device pixel ratio, so a painter written against Flutter's coordinate system
 * draws at the right physical size on any density.</p>
 *
 * <p>Both painters are supported: {@code painter} draws behind the child,
 * {@code foregroundPainter} over it. The child's own components are ordinary
 * siblings attached after the backdrop, so they paint on top of it.</p>
 */
public class CustomPaintRenderElement extends SingleChildRenderElement {

    public CustomPaintRenderElement(CustomPaint widget) {
        super(widget);
    }

    private CustomPaint paintWidget() {
        return (CustomPaint) widget();
    }

    @Override
    protected Widget childWidget() {
        return paintWidget().getChild();
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        return new PainterSurface();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        RenderElement child = renderChild();
        if (child != null) {
            Size cs = child.layout(constraints);
            setChildOffset(child, 0, 0);
            return constraints.constrain(cs);
        }
        // No child: Flutter uses CustomPaint.size (logical pixels), falling
        // back to the largest the constraints allow.
        Size preferred = paintWidget().getSize();
        if (preferred != null) {
            return constraints.constrain(new Size(
                    Dp.px(preferred.width()), Dp.px(preferred.height())));
        }
        return constraints.constrain(new Size(
                constraints.hasBoundedWidth() ? constraints.maxWidth() : 0,
                constraints.hasBoundedHeight() ? constraints.maxHeight() : 0));
    }

    /** The component that hands its Graphics to the painters. */
    private final class PainterSurface extends Container {

        PainterSurface() {
            setUIID("FlutterCustomPaint");
            getAllStyles().setPadding(0, 0, 0, 0);
            getAllStyles().setMargin(0, 0, 0, 0);
            getAllStyles().setBgTransparency(0);
        }

        @Override
        public void paint(Graphics g) {
            run(g, paintWidget().getPainter());
            super.paint(g);
            run(g, paintWidget().getForegroundPainter());
        }

        private void run(Graphics g, CustomPainter painter) {
            if (painter == null) {
                return;
            }
            double dpr = Dp.scale();
            if (dpr <= 0) {
                dpr = 1;
            }
            int clipX = g.getClipX();
            int clipY = g.getClipY();
            int clipW = g.getClipWidth();
            int clipH = g.getClipHeight();
            int color = g.getColor();
            int alpha = g.getAlpha();
            try {
                // the painter's box, in the logical pixels it expects
                Size logical = new Size(getWidth() / dpr, getHeight() / dpr);
                // The origin is this component's PARENT-RELATIVE position, because a Graphics
                // being painted through has already accumulated its ancestors' translation
                // (Container.paintChildren translates by getX()/getY() on the way down) - which
                // is why the whole of Codename One draws with getX(), not getAbsoluteX(). Using
                // the absolute position here added the ancestors' offset a second time and
                // pushed the drawing outside the bounds this component clips to, so the painter
                // ran and nothing appeared.
                painter.paint(new GraphicsCanvas(g, getX(), getY(), dpr), logical);
            } catch (Throwable t) {
                // one misbehaving painter must not take the whole frame down
                Log.p("Flutter runtime: CustomPainter failed: " + t);
            } finally {
                g.setClip(clipX, clipY, clipW, clipH);
                g.setColor(color);
                g.setAlpha(alpha);
            }
        }
    }
}
