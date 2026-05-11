package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.hellocodenameone.tests.BaseTest;

/// Regression test for the chart Canvas alpha-leak fix (commit 4e3f8b47b).
/// Draws the chart-line dataset's two stroked polylines + four opaque-black
/// margin fillRects (mirroring XYChart.draw's drawSeries + drawBackground
/// margin-mask sequence) using raw `g.drawShape` / `g.fillRect` instead of
/// the chart-package compat Canvas. Sized by `getWidth()` / `getHeight()`
/// so it captures the same chart-line geometry on every pipeline (iOS
/// 1179x2556, Android emulator 320x640, JS, JavaSE) and isn't sensitive
/// to platform resolution.
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
            // Mirror ChartComponent.paint's prologue: stash and force AA on.
            boolean oldAA = g.isAntiAliased();
            g.setAntiAliased(true);

            // Use relative coords so the test renders correctly across every
            // platform pipeline: iOS at 1179x2556 native, Android emulator at
            // 320x640, JS at desktop dimensions, JavaSE at simulator. The
            // earlier hard-coded iPhone coords drew off-screen on Android.
            int x = getX();
            int y = getY();
            int viewW = getWidth();
            int viewH = getHeight();
            float marginTop    = viewH * 0.014f;  // matches chart-line frame proportions on iPhone
            float marginBottom = viewH * 0.009f;
            float marginLeft   = viewW * 0.051f;
            float marginRight  = viewW * 0.020f;
            float dataLeft   = x + marginLeft;
            float dataTop    = y + marginTop;
            float dataRight  = x + viewW - marginRight;
            float dataBottom = y + viewH - marginBottom;
            float dataW = dataRight - dataLeft;
            float dataH = dataBottom - dataTop;
            // chart-line dataset: north (12,16,22,18,28), south (8,11,13,16,19).
            // Mapping the 5 (x,y) pairs into [dataLeft..dataRight] x
            // [dataBottom..dataTop] reproduces the non-monotonic Y path that
            // the iOS Stroker / alpha-mask path receives from chart-line,
            // independent of native screen resolution.
            float xStep = dataW / 4f;
            float yScale = dataH / 20f;
            float baseline = dataBottom;

            GeneralPath p1 = new GeneralPath();
            p1.moveTo(dataLeft,                 baseline - (12 - 8) * yScale);
            p1.lineTo(dataLeft + xStep,         baseline - (16 - 8) * yScale);
            p1.lineTo(dataLeft + 2 * xStep,     baseline - (22 - 8) * yScale);
            p1.lineTo(dataLeft + 3 * xStep,     baseline - (18 - 8) * yScale);
            p1.lineTo(dataLeft + 4 * xStep,     baseline - (28 - 8) * yScale);

            GeneralPath p2 = new GeneralPath();
            p2.moveTo(dataLeft,                 baseline - (8  - 8) * yScale);
            p2.lineTo(dataLeft + xStep,         baseline - (11 - 8) * yScale);
            p2.lineTo(dataLeft + 2 * xStep,     baseline - (13 - 8) * yScale);
            p2.lineTo(dataLeft + 3 * xStep,     baseline - (16 - 8) * yScale);
            p2.lineTo(dataLeft + 4 * xStep,     baseline - (19 - 8) * yScale);

            // Same setColor + concatenateAlpha + drawShape pattern that
            // Canvas.applyPaint / canvas.drawPath uses for chart-line's
            // polylines, with the chart's BEVEL join and CAP_BUTT cap.
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

            // Mirror XYChart.draw's 4 unconditional margin fillRects with
            // marginsColor == NO_COLOR (0). ColorUtil.IColor(0) maps alpha 0
            // -> 255 (chart-package historical "0 means opaque" rule), so
            // applyPaint emits setColor(0) + concatenateAlpha(255) +
            // fillRect, i.e. four opaque-black strips around the data area.
            g.setColor(0);
            int marginAlpha = 0;
            if (marginAlpha == 0) {
                marginAlpha = 255;
            }
            g.concatenateAlpha(marginAlpha);
            // bottom strip (under data area)
            g.fillRect(x, (int) dataBottom, viewW, viewH - (int) (dataBottom - y));
            // top strip
            g.fillRect(x, y, viewW, (int) marginTop);
            // left strip (HORIZONTAL orientation default)
            g.fillRect(x, y, (int) (dataLeft - x), viewH);
            // right strip
            g.fillRect((int) dataRight, y, (int) marginRight, viewH);

            g.setAntiAliased(oldAA);
        }
    }

}
