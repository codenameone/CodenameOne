package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;

public class GraphicsTransformationsScreenshotTest extends AbstractGraphicsScreenshotTest {
    @Override
    protected Component createContent() {
        return new TransformCanvas();
    }

    @Override
    protected String screenshotName() {
        return "GraphicsTransformations";
    }

    private static final class TransformCanvas extends Component {
        private Image marker;

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

            paintTransforms(g, margin, margin, columnWidth, rowHeight);
            paintAffineOperations(g, margin * 2 + columnWidth, margin, columnWidth, rowHeight);
            paintShapeAndClipSupport(g, margin, margin * 2 + rowHeight, columnWidth, rowHeight);
            paintNativeAndScale(g, margin * 2 + columnWidth, margin * 2 + rowHeight, columnWidth, rowHeight);
        }

        private void paintTransforms(Graphics g, int x, int y, int w, int h) {
            g.setColor(0x111827);
            g.fillRect(x, y, w, h);
            g.setColor(0x334155);
            g.drawRect(x, y, w - 1, h - 1);

            Transform original = Transform.makeIdentity();
            g.getTransform(original);
            Transform legacy = g.getTransform();
            boolean transformSupported = g.isTransformSupported();
            boolean perspectiveSupported = g.isPerspectiveTransformSupported();

            Transform translate = Transform.makeTranslation(40, 30);
            Transform rotation = Transform.makeRotation((float) Math.toRadians(20), 0, 0);
            translate.concatenate(rotation);
            if (transformSupported) {
                g.transform(translate);
            }
            g.setColor(0x22c55e);
            g.fillRect(x + 10, y + 10, 120, 50);
            g.setColor(0xf97316);
            g.drawRect(x + 6, y + 6, 128, 58);
            g.setTransform(original);

            g.setColor(0xe2e8f0);
            g.drawString("transform ok=" + transformSupported + " persp=" + perspectiveSupported, x + 10, y + h - 24);
            g.drawString("legacy m00=" + legacy.getMatrix()[0], x + 10, y + h - 44);
        }

        private void paintAffineOperations(Graphics g, int x, int y, int w, int h) {
            g.setColor(0x0f172a);
            g.fillRect(x, y, w, h);
            g.setColor(0x475569);
            g.drawRect(x, y, w - 1, h - 1);

            int cx = x + w / 2;
            int cy = y + h / 2;
            g.translate(cx, cy);
            if (marker == null) {
                marker = Image.createImage(60, 60, 0xfff8fafc);
                Graphics mg = marker.getGraphics();
                mg.setColor(0x2563eb);
                mg.fillRect(8, 8, 44, 44);
            }

            if (g.isAffineSupported()) {
                g.scale(1.1f, 0.8f);
                g.rotate((float) Math.toRadians(12));
                g.drawImage(marker, -30, -30);
                g.rotateRadians((float) Math.toRadians(14));
                g.shear(0.2f, 0.1f);
                g.drawImage(marker, 10, 8, 48, 48);
                g.resetAffine();
                g.rotate((float) Math.toRadians(8), 0, 0);
                g.rotateRadians((float) Math.toRadians(-12), 0, 0);
                g.scale(0.9f, 0.9f);
                g.rotate((float) Math.toRadians(18), 12, 12);
                g.rotateRadians((float) Math.toRadians(-10), -8, -8);
                g.shear(-0.15f, 0);
                g.drawImage(marker, -12, 26);
                g.resetAffine();
            }
            g.translate(-cx, -cy);
            g.setColor(0xf8fafc);
            g.drawString("affine scaleX=" + g.getScaleX() + " scaleY=" + g.getScaleY(), x + 10, y + h - 24);
        }

        private void paintShapeAndClipSupport(Graphics g, int x, int y, int w, int h) {
            g.setColor(0x0f172a);
            g.fillRect(x, y, w, h);
            g.setColor(0x334155);
            g.drawRect(x, y, w - 1, h - 1);

            boolean shapeSupported = g.isShapeSupported();
            boolean shapeClipSupported = g.isShapeClipSupported();
            GeneralPath path = new GeneralPath();
            path.moveTo(x + 16, y + 16);
            path.lineTo(x + w - 16, y + 28);
            path.lineTo(x + 24, y + h - 20);
            path.closePath();
            if (shapeSupported) {
                g.setColor(0xa855f7);
                g.fillShape(path);
            }

            Rectangle rectClip = new Rectangle(x + w / 3, y + h / 3, w / 2, h / 3);
            if (shapeClipSupported) {
                g.setClip(path);
            } else {
                g.setClip(rectClip.getX(), rectClip.getY(), rectClip.getSize().getWidth(), rectClip.getSize().getHeight());
            }
            g.setColor(0x22d3ee);
            g.fillRect(x, y, w, h);
            g.setClip(0, 0, getWidth(), getHeight());
            g.setColor(0xe2e8f0);
            g.drawString("shape?=" + shapeSupported + " clip?=" + shapeClipSupported, x + 10, y + h - 24);
        }

        private void paintNativeAndScale(Graphics g, int x, int y, int w, int h) {
            g.setColor(0x111827);
            g.fillRect(x, y, w, h);
            g.setColor(0x475569);
            g.drawRect(x, y, w - 1, h - 1);

            Object nativeHandle = g.beginNativeGraphicsAccess();
            g.setColor(0x22c55e);
            g.fillRect(x + 12, y + 12, w / 3, 48);
            g.endNativeGraphicsAccess();

            g.setColor(0xf97316);
            g.drawString("native handle=" + (nativeHandle != null), x + 10, y + h - 48);
            g.drawString("render hints=" + g.getRenderingHints(), x + 10, y + h - 28);
        }
    }
}
