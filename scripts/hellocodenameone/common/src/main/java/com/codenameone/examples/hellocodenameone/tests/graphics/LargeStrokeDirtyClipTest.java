package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.hellocodenameone.tests.BaseTest;

/// Minimal reproduction of the iOS chart-line blank-render: a single
/// Component in BorderLayout.CENTER that draws a large stroked
/// GeneralPath via `g.drawShape(...)` during its initial paint and then
/// never invalidates itself again. No UITimer / animation / peer view --
/// the form is shown, the slide-in transition completes, and nothing
/// else triggers a repaint until the screenshot fires 1500ms later.
///
/// graphics-draw-shape already proves drawShape itself works on iOS
/// (its 2x2 grid renders correctly on GL+Metal). What's different here
/// is that the form has NO continuous activity after the initial paint.
/// chart-line shows the same pattern: the chart's paint cycle ends at
/// the end of the slide-in transition and nothing else queues a paint.
/// On iOS GL+Metal the captured PNG of chart-line is uniform body-bg
/// with even the form title bar missing -- something in iOS's
/// presentDrawable / CALayer compositor pipeline is failing to keep
/// the last-presented frame visible when no further paints arrive.
///
/// If THIS test renders the polyline correctly, the bug is specific to
/// ChartComponent's paint cycle and we'll need to look at what it does
/// differently from a vanilla `Component.paint` (transform handling,
/// Canvas wrapper layer, multi-call drawText/drawLine sequence, etc).
/// If THIS test goes blank, ChartComponent is innocent and the bug is
/// in iOS Metal / GL's idle-frame compositor handling.
public class LargeStrokeDirtyClipTest extends BaseTest {

    @Override
    public boolean runTest() throws Exception {
        Form form = createForm(screenshotName(), new BorderLayout(), screenshotName());
        // Single component in BorderLayout.CENTER -- mirrors the chart-line
        // scenario exactly: form is shown, the painter draws once during
        // the slide-in transition, and after that nothing else queues a
        // repaint. No UITimer-driven heartbeat. If iOS captures a blank
        // PNG from this minimal setup we've reproduced the chart-line
        // failure without the chart-package; if it captures the polyline,
        // ChartComponent is doing something specific that other Component
        // subclasses don't.
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
            // Mirror XYChart's drawSeries: two stroked polylines back to
            // back, both spanning the full component width. chart-line
            // has 2 series (north + south) and emits two drawShape calls
            // sharing the same Stroke / coordinate space; the previous
            // single-polyline version of this test (8926797c9) rendered
            // fine on iOS Metal so the ONE drawShape path isn't broken.
            // If TWO drawShape calls in sequence reproduce the blank,
            // we've isolated the iOS edge case to either the Stroker /
            // alpha-mask creation collision or the encoder state between
            // two consecutive drawShape ops.
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();

            GeneralPath p1 = new GeneralPath();
            p1.moveTo(x + w * 0.05f, y + h * 0.85f);
            p1.lineTo(x + w * 0.25f, y + h * 0.70f);
            p1.lineTo(x + w * 0.45f, y + h * 0.55f);
            p1.lineTo(x + w * 0.65f, y + h * 0.40f);
            p1.lineTo(x + w * 0.85f, y + h * 0.20f);

            GeneralPath p2 = new GeneralPath();
            p2.moveTo(x + w * 0.05f, y + h * 0.95f);
            p2.lineTo(x + w * 0.25f, y + h * 0.92f);
            p2.lineTo(x + w * 0.45f, y + h * 0.86f);
            p2.lineTo(x + w * 0.65f, y + h * 0.79f);
            p2.lineTo(x + w * 0.85f, y + h * 0.74f);

            Stroke stroke = new Stroke(3f, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 1f);
            g.setColor(0x0a66ff);
            g.drawShape(p1, stroke);
            g.setColor(0xee4a4a);
            g.drawShape(p2, stroke);
        }
    }

}
