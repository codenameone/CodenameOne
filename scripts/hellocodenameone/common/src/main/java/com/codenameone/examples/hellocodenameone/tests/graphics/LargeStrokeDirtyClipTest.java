package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.hellocodenameone.tests.BaseTest;

/// Standalone reproduction of the iOS form-Graphics edge case the chart
/// screenshot tests hit -- the existing graphics-draw-shape test ALREADY
/// proves drawShape itself works on iOS, so the bug must be in the
/// interaction between paintDirty's per-component dirty-region clip and
/// a fillRect (the form's bg paintBackground heartbeat) that collides
/// with the iOS port's internal graphics state mid-frame.
///
/// The chart-line / chart-bar / chart-scatter pattern is:
///   1. Component A (the chart) is painted ONCE during the slide-in
///      transition. drawShape writes alpha-mask textures into the
///      screenTexture.
///   2. Component A is never repainted -- its bounds aren't dirty after
///      the transition completes.
///   3. The form keeps repainting at ~50fps because something else (a
///      timer, an animation, a peer view, etc) marks a small dirty
///      region. paintDirty restricts the wrapper's clip to that small
///      region, but the form's bg fillRect command is still queued at
///      the form's full bounds.
///   4. Eventually Component A's pixels are wiped from the screenTexture
///      and the captured PNG is uniform body-bg.
///
/// This test reproduces the same pattern in isolation:
///   - Component A draws a large stroked GeneralPath via drawShape
///     during its initial paint, then never invalidates itself again.
///   - Component B is a tiny strip in the bottom-left that a UITimer
///     forces to repaint every 100ms via repaint() -- mimicking the
///     "something else stays dirty" condition that keeps paintDirty
///     running.
/// If Component A's polyline is visible in the captured PNG the bug is
/// chart-package-specific. If Component A's polyline gets wiped (the
/// PNG shows only the form chrome and the tiny B strip) we've isolated
/// the dirty-region/fillRect interaction the iOS port needs to fix.
public class LargeStrokeDirtyClipTest extends BaseTest {

    @Override
    public boolean runTest() throws Exception {
        Form form = createForm(screenshotName(), new BorderLayout(), screenshotName());
        LargeStrokeComponent painter = new LargeStrokeComponent();
        TickerComponent ticker = new TickerComponent();
        form.add(BorderLayout.CENTER, painter);
        form.add(BorderLayout.SOUTH, ticker);
        form.show();
        // Drive the dirty-region heartbeat: every 100ms invalidate just
        // the ticker (a tiny strip), so paintDirty fires with a small
        // dirty clip while Component A's bounds stay un-invalidated.
        // Mirrors the condition under which the chart screenshot tests
        // lose their pixels on iOS GL+Metal.
        UITimer.timer(100, true, form, () -> ticker.bumpAndRepaint());
        return true;
    }

    protected String screenshotName() {
        return "graphics-large-stroke-dirty-clip";
    }

    private static final class LargeStrokeComponent extends Component {

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (!g.isShapeSupported()) {
                return;
            }
            // Polyline shaped like an XYChart series line: full component
            // width, ~5 segments, deeper into the component bounds than
            // the existing graphics-draw-shape's per-cell triangle.
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            GeneralPath p = new GeneralPath();
            p.moveTo(x + w * 0.05f, y + h * 0.85f);
            p.lineTo(x + w * 0.25f, y + h * 0.70f);
            p.lineTo(x + w * 0.45f, y + h * 0.55f);
            p.lineTo(x + w * 0.65f, y + h * 0.40f);
            p.lineTo(x + w * 0.85f, y + h * 0.20f);
            g.setColor(0x0a66ff);
            g.drawShape(p, new Stroke(3f, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 1f));
        }
    }

    private static final class TickerComponent extends Component {
        private int tick;

        @Override
        protected com.codename1.ui.geom.Dimension calcPreferredSize() {
            // Small fixed strip so its dirty region stays much smaller
            // than the painter component above.
            return new com.codename1.ui.geom.Dimension(
                    CN.convertToPixels(40, true),
                    CN.convertToPixels(8, false));
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            // Draw a tiny block whose position cycles, so successive
            // paints actually invalidate. We don't strictly need visible
            // motion -- the per-frame repaint() is what triggers the
            // iOS-side dirty-region paintDirty path.
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            int phase = tick & 0x7;
            int bx = x + (phase * w) / 8;
            g.setColor(0x4a90e2);
            g.fillRect(bx, y, Math.max(2, w / 16), h);
        }

        void bumpAndRepaint() {
            tick++;
            repaint();
        }
    }
}
