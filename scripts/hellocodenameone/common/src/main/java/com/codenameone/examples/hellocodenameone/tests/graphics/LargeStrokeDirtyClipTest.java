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
            // Mirror ChartComponent.paint's exact prologue: stash AA and
            // force-enable it. ChartComponent does this even though the
            // existing graphics-draw-shape test toggles AA without issue;
            // the chart's pattern is set-AA-then-many-draws which the
            // graphics tests don't replicate.
            boolean oldAA = g.isAntiAliased();
            g.setAntiAliased(true);
            int x = getX();
            int y = getY();

            // Hard-code the exact path coordinates XYChart.drawSeries
            // produces for chart-line's data (years 2018-2022, north
            // values 12,16,22,18,28; south values 8,11,13,16,19) on
            // iPhone 16 portrait. Non-monotonic Y path is the chart-line
            // case the previous monotonic-up test (1b000bdb4) didn't
            // exercise, and the BEVEL join + 1.0 miter limit plus
            // fractional X coords (xPxPerUnit ~= 262.75) match what the
            // iOS Stroker actually receives from chart-line. If THIS
            // version reproduces the blank, the bug is in the
            // Stroker / alpha-mask path for non-monotonic + fractional
            // strokes; if it renders, ChartComponent is wrapping or
            // setting Graphics state we haven't matched yet.
            float left = x + 102f;
            float bottom = y + 1714f;
            float xStep = 262.75f;
            float yScale = 83.8f;

            GeneralPath p1 = new GeneralPath();
            p1.moveTo(left,             bottom - (12 - 8) * yScale);
            p1.lineTo(left + xStep,     bottom - (16 - 8) * yScale);
            p1.lineTo(left + 2 * xStep, bottom - (22 - 8) * yScale);
            p1.lineTo(left + 3 * xStep, bottom - (18 - 8) * yScale);
            p1.lineTo(left + 4 * xStep, bottom - (28 - 8) * yScale);

            GeneralPath p2 = new GeneralPath();
            p2.moveTo(left,             bottom - (8  - 8) * yScale);
            p2.lineTo(left + xStep,     bottom - (11 - 8) * yScale);
            p2.lineTo(left + 2 * xStep, bottom - (13 - 8) * yScale);
            p2.lineTo(left + 3 * xStep, bottom - (16 - 8) * yScale);
            p2.lineTo(left + 4 * xStep, bottom - (19 - 8) * yScale);

            // Mirror Canvas.applyPaint + canvas.drawPath: a NEW Stroke
            // is constructed per drawShape call (compat/Canvas.java
            // getStroke()), and applyPaint runs setColor + a
            // concatenateAlpha(alpha) before each draw. The Canvas
            // wrapper preserves these per-shape allocations in
            // chart-line; if creating a fresh Stroke per draw or the
            // concatenateAlpha call interacts badly with iOS's
            // textureCache / encoder state between two drawShape ops,
            // this is where the bug surfaces.
            int color1 = 0xff0a66ff;
            g.setColor(color1);
            int alpha1 = (color1 >>> 24) & 0xff;
            if (alpha1 == 0) {
                alpha1 = 255;
            }
            g.concatenateAlpha(alpha1);
            g.drawShape(p1, new Stroke(3f, Stroke.CAP_BUTT, Stroke.JOIN_BEVEL, 1f));

            int color2 = 0xffee4a4a;
            g.setColor(color2);
            int alpha2 = (color2 >>> 24) & 0xff;
            if (alpha2 == 0) {
                alpha2 = 255;
            }
            g.concatenateAlpha(alpha2);
            g.drawShape(p2, new Stroke(3f, Stroke.CAP_BUTT, Stroke.JOIN_BEVEL, 1f));

            g.setAntiAliased(oldAA);
        }
    }

}
