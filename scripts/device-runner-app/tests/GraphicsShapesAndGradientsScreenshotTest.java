package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.GeneralPath;

public class GraphicsShapesAndGradientsScreenshotTest extends AbstractGraphicsScreenshotTest {
    @Override
    protected Component createContent() {
        return new ShapesAndGradientsCanvas();
    }

    @Override
    protected String screenshotName() {
        return "GraphicsShapesAndGradients";
    }

    private static final class ShapesAndGradientsCanvas extends Component {
        private Image patternedImage;

        @Override
        protected Dimension calcPreferredSize() {
            int w = Math.max(Display.getInstance().getDisplayWidth(), 520);
            int h = Math.max(Display.getInstance().getDisplayHeight(), 760);
            return new Dimension(w, h);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(0x0b1220);
            g.fillRect(0, 0, w, h);

            int margin = 12;
            int columnWidth = (w - margin * 3) / 2;
            int rowHeight = (h - margin * 3) / 2;

            paintRectanglesAndLines(g, margin, margin, columnWidth, rowHeight);
            paintPolygonsAndGradients(g, margin * 2 + columnWidth, margin, columnWidth, rowHeight);
            paintImagesAndShadows(g, margin, margin * 2 + rowHeight, columnWidth, rowHeight);
            paintShapesAndTriangles(g, margin * 2 + columnWidth, margin * 2 + rowHeight, columnWidth, rowHeight);
        }

        private void paintRectanglesAndLines(Graphics g, int x, int y, int w, int h) {
            g.setColor(0x111827);
            g.fillRect(x, y, w, h);
            g.setColor(0x334155);
            g.drawRect(x, y, w - 1, h - 1, 2);

            g.setColor(0xf97316);
            g.drawLine(x + 8, y + 12, x + w - 8, y + 12);
            g.setColor(0x22c55e);
            g.fillRect(x + 8, y + 20, w / 2, 36);
            g.setColor(0xf59e0b);
            g.fillRect(x + w / 2 + 12, y + 20, w / 2 - 20, 36, (byte) 120);

            g.setColor(0x38bdf8);
            g.drawRect(x + 8, y + 64, w / 3, 40);
            g.setColor(0x8b5cf6);
            g.drawRect(x + w / 3 + 16, y + 64, w / 3, 40, 4);

            g.setColor(0x16a34a);
            g.fillRoundRect(x + 8, y + 112, w / 3, 46, 16, 16);
            g.setColor(0xec4899);
            g.drawRoundRect(x + w / 3 + 16, y + 112, w / 3, 46, 12, 12);

            g.setColor(0xf43f5e);
            g.fillArc(x + 8, y + 168, 70, 70, 30, 240);
            g.setColor(0x0ea5e9);
            g.drawArc(x + w / 3 + 16, y + 168, 70, 70, 200, 140);

            g.setColor(0xf8fafc);
            g.drawString("rects + arcs", x + 10, y + h - 24);
        }

        private void paintPolygonsAndGradients(Graphics g, int x, int y, int w, int h) {
            g.setColor(0x0f172a);
            g.fillRect(x, y, w, h);
            g.setColor(0x475569);
            g.drawRect(x, y, w - 1, h - 1);

            int[] px = new int[] {x + 16, x + 60, x + 32, x + 68, x + 22};
            int[] py = new int[] {y + 16, y + 20, y + 44, y + 64, y + 60};
            g.setColor(0xf87171);
            g.fillPolygon(px, py, px.length);
            g.setColor(0x10b981);
            g.drawPolygon(px, py, px.length);

            g.setColor(0x22d3ee);
            g.fillTriangle(x + w - 110, y + 16, x + w - 40, y + 20, x + w - 60, y + 74);

            g.fillLinearGradient(0x22d3ee, 0x2563eb, x + 10, y + 90, w - 20, 60, true);
            g.fillLinearGradient(0xf472b6, 0xf59e0b, x + 10, y + 154, w - 20, 60, false);

            g.fillRadialGradient(0x34d399, 0x0ea5e9, x + 12, y + 220, w / 2 - 16, h - 232);
            g.fillRadialGradient(0xf43f5e, 0xc084fc, x + w / 2, y + 220, w / 2 - 16, h - 232, 30, 220);
            g.fillRectRadialGradient(0x2563eb, 0x22d3ee, x + 20, y + h - 80, w - 40, 64, 0.4f, 0.5f, 0.8f);

            g.setColor(0xe2e8f0);
            g.drawString("polygons + gradients", x + 10, y + h - 24);
        }

        private void paintImagesAndShadows(Graphics g, int x, int y, int w, int h) {
            g.setColor(0x111827);
            g.fillRect(x, y, w, h);
            g.setColor(0x475569);
            g.drawRect(x, y, w - 1, h - 1);

            if (patternedImage == null) {
                patternedImage = Image.createImage(72, 72, 0xfff1f5f9);
                Graphics imgG = patternedImage.getGraphics();
                imgG.setColor(0x1d4ed8);
                imgG.fillRect(6, 6, 60, 60);
                imgG.setColor(0xf97316);
                imgG.fillRect(18, 18, 18, 18);
                imgG.setColor(0x0ea5e9);
                imgG.drawRect(0, 0, 71, 71);
            }

            g.drawImage(patternedImage, x + 12, y + 12);
            g.drawImage(patternedImage, x + 120, y + 16, 60, 60);
            g.tileImage(patternedImage, x + 12, y + 86, w - 24, 60);

            g.drawShadow(patternedImage, x + w / 2, y + 12, 6, 12, 8, 4, 0xcc0f172a, 0.45f);
            g.clearRect(x + w / 2 + 12, y + 90, w / 2 - 24, 28);

            g.setColor(0xf8fafc);
            g.drawString("images + shadow", x + 10, y + h - 24);
        }

        private void paintShapesAndTriangles(Graphics g, int x, int y, int w, int h) {
            g.setColor(0x0f172a);
            g.fillRect(x, y, w, h);
            g.setColor(0x334155);
            g.drawRect(x, y, w - 1, h - 1);

            GeneralPath shape = new GeneralPath();
            shape.moveTo(x + 12, y + 12);
            shape.lineTo(x + w / 2f, y + 60);
            shape.lineTo(x + 18, y + 108);
            shape.closePath();
            if (g.isShapeSupported()) {
                g.setColor(0x22c55e);
                g.fillShape(shape);
                g.setColor(0xf97316);
                g.drawShape(shape, new Stroke(3, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 1f));
            }

            g.setColor(0x38bdf8);
            g.fillTriangle(x + w - 120, y + 14, x + w - 24, y + 48, x + w - 96, y + 104);
            g.setColor(0xf43f5e);
            g.drawLine(x + w - 110, y + h - 68, x + w - 24, y + h - 68);

            g.setColor(0xe2e8f0);
            g.drawString("shapes + stroke", x + 10, y + h - 24);
        }
    }
}
