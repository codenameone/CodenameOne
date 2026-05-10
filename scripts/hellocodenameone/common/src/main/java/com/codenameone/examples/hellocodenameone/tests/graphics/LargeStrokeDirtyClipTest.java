package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.hellocodenameone.tests.BaseTest;

/// Standalone reproduction of the iOS form-Graphics edge case where a large
/// stroked path drawn ONCE during the form's slide-in transition disappears
/// from the captured screenshot, even though the chart-package's
/// AbstractGraphicsScreenshotTest pattern (multiple panels in a 2x2 grid)
/// renders correctly on the same harness.
///
/// The bug surfaces in the chart screenshot tests
/// (`chart-line` / `chart-bar` / `chart-scatter` etc) on iOS GL+Metal:
/// `LineChart.drawSeries` -> `AbstractChart.drawPath` ->
/// `g.drawShape(path, stroke)` paints alpha-mask textures the size of the
/// chart's bounds (e.g. 1054x1342 for a BorderLayout.CENTER chart on iPhone
/// 16) into the screenTexture during the slide-in. After the transition
/// completes, no more chart paints fire (no dirty region invalidates the
/// chart's bounds), but the form's bg-fillRect heartbeat keeps queuing
/// full-form fillRect ops at ~50fps. With Metal's MTLLoadActionLoad the
/// expectation is that those bg fillRects are clipped to whatever small
/// dirty region the queued component declared -- yet the captured PNG
/// is uniform body-bg colour with not even the form title bar visible,
/// suggesting the clip path silently lets a large alpha-mask op be
/// overwritten by a subsequent paint.
///
/// This test isolates the same paint pattern WITHOUT the chart-package:
///   - Form with a single Component in BorderLayout.CENTER
///   - The Component's paint draws a large stroked GeneralPath via
///     `g.drawShape(path, stroke)` -- the same primitive XYChart uses
///     for line strokes.
/// If iOS captures a non-blank PNG with the stroked path visible, the bug
/// is specific to ChartComponent (not the underlying drawShape /
/// dirty-region path); if iOS captures a blank PNG, we have a minimal
/// reproduction and can iterate the iOS-port fix against it.
public class LargeStrokeDirtyClipTest extends BaseTest {

    @Override
    public boolean runTest() throws Exception {
        Form form = createForm(screenshotName(), new BorderLayout(), screenshotName());
        form.add(BorderLayout.CENTER, new LargeStrokeComponent());
        form.show();
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
            // Build a path roughly the same shape XYChart's drawSeries
            // produces: a polyline that spans the whole component bounds,
            // ~5 segments, from upper-left to lower-right with one mid
            // bend. Stroked with a 3px line. The alpha-mask texture for
            // this path is component-bounds-sized (~1051x1676 on iPhone 16
            // landscape-portrait), matching what the chart-line failure
            // produces.
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
}
