package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.testing.AbstractTest;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;

public class GraphicsPipelineScreenshotTest extends AbstractTest {
    @Override
    public boolean runTest() throws Exception {
        final Form[] formHolder = new Form[1];
        Cn1ssDeviceRunnerHelper.runOnEdtSync(() -> {
            Form form = new Form("Graphics Pipeline", new BorderLayout());
            form.add(BorderLayout.CENTER, new GraphicsShowcase());
            formHolder[0] = form;
            form.show();
        });

        Cn1ssDeviceRunnerHelper.waitForMillis(1200);

        final boolean[] result = new boolean[1];
        Cn1ssDeviceRunnerHelper.runOnEdtSync(() -> {
            if (formHolder[0] != null) {
                formHolder[0].revalidate();
                formHolder[0].repaint();
            }
            result[0] = Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot("GraphicsPipeline");
        });
        return result[0];
    }

    private static final class GraphicsShowcase extends Component {
        @Override
        protected Dimension calcPreferredSize() {
            int w = Math.max(Display.getInstance().getDisplayWidth(), 420);
            int h = Math.max(Display.getInstance().getDisplayHeight(), 720);
            return new Dimension(w, h);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(0x0b1220);
            g.fillRect(0, 0, w, h);

            int margin = 16;
            int columnWidth = (w - margin * 3) / 2;
            int rowHeight = (h - margin * 3) / 2;

            drawBasicPrimitives(g, margin, margin, columnWidth, rowHeight);
            drawTransforms(g, margin * 2 + columnWidth, margin, columnWidth, rowHeight);
            drawClipAndAlpha(g, margin, margin * 2 + rowHeight, columnWidth, rowHeight);
            drawGradients(g, margin * 2 + columnWidth, margin * 2 + rowHeight, columnWidth, rowHeight);
        }

        private void drawBasicPrimitives(Graphics g, int x, int y, int w, int h) {
            g.setColor(0x111827);
            g.fillRect(x, y, w, h);
            g.setColor(0x475569);
            g.drawRect(x, y, w - 1, h - 1);

            g.setColor(0xfacc15);
            g.drawLine(x + 8, y + 8, x + w - 8, y + 8);

            g.setColor(0x22c55e);
            g.fillRect(x + 8, y + 18, w / 3, 36);
            g.setColor(0xf97316);
            g.drawRect(x + w / 3 + 16, y + 18, w / 3, 36);

            g.setColor(0x38bdf8);
            g.fillRoundRect(x + 8, y + 62, w / 3, 40, 14, 14);
            g.setColor(0xec4899);
            g.drawRoundRect(x + w / 3 + 16, y + 62, w / 3, 40, 18, 18);

            g.setColor(0xa855f7);
            g.fillArc(x + 8, y + 108, 70, 70, 30, 210);
            g.setColor(0x6366f1);
            g.drawArc(x + w / 3 + 16, y + 108, 70, 70, 200, 300);

            int[] px = new int[] {x + w - 78, x + w - 34, x + w - 58};
            int[] py = new int[] {y + 24, y + 24, y + 70};
            g.setColor(0x7dd3fc);
            g.fillPolygon(px, py, px.length);
            g.setColor(0x0ea5e9);
            g.drawPolygon(px, py, px.length);

            g.setColor(0xe5e7eb);
            g.drawString("basic primitives", x + 10, y + h - 28);
        }

        private void drawTransforms(Graphics g, int x, int y, int w, int h) {
            g.setColor(0x0f172a);
            g.fillRect(x, y, w, h);
            g.setColor(0x334155);
            g.drawRect(x, y, w - 1, h - 1);

            int cx = x + w / 2;
            int cy = y + h / 2;

            g.translate(cx, cy);
            g.setColor(0x22c55e);
            g.fillRect(-50, -22, 100, 44);

            if (g.isAffineSupported()) {
                g.rotateRadians((float) Math.toRadians(18));
                g.scale(0.85f, 0.85f);
                g.setColor(0xf97316);
                g.fillRoundRect(-64, -30, 128, 60, 16, 16);
                g.shear(0.25f, 0);
                g.setColor(0x38bdf8);
                g.drawRect(-70, -36, 140, 72);
                g.resetAffine();
            }

            g.setColor(0xf8fafc);
            g.drawString("translate + affine", -58, 4);
            g.translate(-cx, -cy);

            g.setColor(0x14b8a6);
            g.drawArc(x + 10, y + h - 70, 54, 54, 30, 280);
            g.setColor(0xf43f5e);
            g.fillArc(x + w - 78, y + h - 78, 64, 64, 200, 140);
        }

        private void drawClipAndAlpha(Graphics g, int x, int y, int w, int h) {
            g.setColor(0x0f172a);
            g.fillRect(x, y, w, h);
            g.setColor(0x475569);
            g.drawRect(x, y, w - 1, h - 1);

            int oldClipX = g.getClipX();
            int oldClipY = g.getClipY();
            int oldClipW = g.getClipWidth();
            int oldClipH = g.getClipHeight();

            g.setClip(x + 10, y + 10, w - 20, h - 60);
            g.fillLinearGradient(0x22d3ee, 0x2563eb, x + 10, y + 10, w - 20, h - 60, false);

            g.setAlpha(150);
            g.setColor(0xfbbf24);
            g.fillRect(x + 24, y + 24, w / 2, 54);
            g.setColor(0x10b981);
            g.fillRect(x + w / 3, y + 40, w / 2, 54);
            g.setAlpha(255);

            g.setClip(oldClipX, oldClipY, oldClipW, oldClipH);
            g.setColor(0xe5e7eb);
            g.drawString("clip + alpha", x + 10, y + h - 28);
        }

        private void drawGradients(Graphics g, int x, int y, int w, int h) {
            g.setColor(0x0f172a);
            g.fillRect(x, y, w, h);
            g.setColor(0x334155);
            g.drawRect(x, y, w - 1, h - 1);

            g.fillLinearGradient(0xf472b6, 0xc084fc, x + 8, y + 12, w - 16, 72, true);
            g.fillRadialGradient(0x22c55e, 0x0ea5e9, x + 12, y + 96, w / 2 - 18, h - 112);
            g.fillRectRadialGradient(0xf43f5e, 0xf59e0b, x + w / 2, y + 96, w / 2 - 12, h - 112, 0.3f, 0.5f, 0.8f);

            g.setColor(0xe5e7eb);
            g.drawString("linear + radial gradients", x + 10, y + h - 28);
        }
    }
}
