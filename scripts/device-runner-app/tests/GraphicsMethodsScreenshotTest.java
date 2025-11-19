package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.testing.AbstractTest;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;

public class GraphicsMethodsScreenshotTest extends AbstractTest {
    @Override
    public boolean runTest() throws Exception {
        String[] methods = GRAPHICS_METHODS;
        final MethodCoverageCanvas canvas = new MethodCoverageCanvas();
        final Form[] formHolder = new Form[1];

        Cn1ssDeviceRunnerHelper.runOnEdtSync(() -> {
            Form form = new Form("Graphics API", new BorderLayout());
            form.add(BorderLayout.CENTER, canvas);
            formHolder[0] = form;
            form.show();
        });

        // Allow layout to settle before we start iterating through methods.
        Cn1ssDeviceRunnerHelper.waitForMillis(500);

        final boolean[] success = new boolean[] {true};
        int index = 1;
        int total = methods.length;
        for (int i = 0; i < total; i++) {
            String descriptor = methods[i];
            final String label = descriptor + " (" + index + "/" + total + ")";
            Cn1ssDeviceRunnerHelper.runOnEdtSync(() -> {
                canvas.setHighlightedMethod(label);
                if (formHolder[0] != null) {
                    formHolder[0].revalidate();
                    formHolder[0].repaint();
                }
            });

            // Give the EDT a short window to repaint before taking the screenshot.
            Cn1ssDeviceRunnerHelper.waitForMillis(160);

            final boolean[] shotOk = new boolean[1];
            final String screenshotName = "Graphics." + Cn1ssDeviceRunnerHelper.sanitizeTestName(descriptor);
            Cn1ssDeviceRunnerHelper.runOnEdtSync(() -> shotOk[0] = Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot(screenshotName));
            if (!shotOk[0]) {
                success[0] = false;
            }
            index++;
        }

        return success[0];
    }

    private static final String[] GRAPHICS_METHODS = new String[] {
            "translate(int,int)",
            "getTranslateX()",
            "getTranslateY()",
            "getColor()",
            "setColor(int)",
            "setColor(Paint)",
            "getPaint()",
            "setAndGetColor(int)",
            "getFont()",
            "setFont(Font)",
            "getClipX()",
            "getClip()",
            "setClip(int[])",
            "setClip(Shape)",
            "getClipY()",
            "getClipWidth()",
            "getClipHeight()",
            "clipRect(int,int,int,int)",
            "setClip(int,int,int,int)",
            "pushClip()",
            "popClip()",
            "drawLine(int,int,int,int)",
            "fillRect(int,int,int,int)",
            "drawShadow(Image,int,int,int,int,int,int,int,float)",
            "clearRect(int,int,int,int)",
            "drawRect(int,int,int,int)",
            "drawRect(int,int,int,int,int)",
            "drawRoundRect(int,int,int,int,int,int)",
            "lighterColor(int)",
            "darkerColor(int)",
            "fillRoundRect(int,int,int,int,int,int)",
            "fillArc(int,int,int,int,int,int)",
            "drawArc(int,int,int,int,int,int)",
            "drawString(String,int,int,int)",
            "drawStringBaseline(String,int,int)",
            "drawStringBaseline(String,int,int,int)",
            "drawString(String,int,int)",
            "drawChar(char,int,int)",
            "drawChars(char[],int,int,int,int)",
            "drawImage(Image,int,int)",
            "drawImage(Image,int,int,int,int)",
            "drawShape(Shape,Stroke)",
            "fillShape(Shape)",
            "isTransformSupported()",
            "isPerspectiveTransformSupported()",
            "isShapeSupported()",
            "isShapeClipSupported()",
            "transform(Transform)",
            "getTransform()",
            "setTransform(Transform)",
            "getTransform(Transform)",
            "fillTriangle(int,int,int,int,int,int)",
            "fillRadialGradient(int,int,int,int,int,int)",
            "fillRadialGradient(int,int,int,int,int,int,int,int)",
            "fillRectRadialGradient(int,int,int,int,int,int,float,float,float)",
            "fillLinearGradient(int,int,int,int,int,int,boolean)",
            "fillRect(int,int,int,int,byte)",
            "fillPolygon(int[],int[],int)",
            "drawPolygon(int[],int[],int)",
            "isAlphaSupported()",
            "setAndGetAlpha(int)",
            "concatenateAlpha(int)",
            "getAlpha()",
            "setAlpha(int)",
            "isAntiAliasingSupported()",
            "isAntiAliasedTextSupported()",
            "isAntiAliased()",
            "setAntiAliased(boolean)",
            "isAntiAliasedText()",
            "setAntiAliasedText(boolean)",
            "isAffineSupported()",
            "resetAffine()",
            "scale(float,float)",
            "rotate(float)",
            "rotateRadians(float)",
            "rotate(float,int,int)",
            "rotateRadians(float,int,int)",
            "shear(float,float)",
            "beginNativeGraphicsAccess()",
            "endNativeGraphicsAccess()",
            "tileImage(Image,int,int,int,int)",
            "getScaleX()",
            "getScaleY()",
            "getRenderingHints()",
            "setRenderingHints(int)"
    };

    private static final class MethodCoverageCanvas extends Component {
        private String highlightedMethod = "";
        private Image cachedLogo;

        void setHighlightedMethod(String method) {
            this.highlightedMethod = method == null ? "" : method;
            repaint();
        }

        @Override
        protected Dimension calcPreferredSize() {
            int w = Math.max(Display.getInstance().getDisplayWidth(), 540);
            int h = Math.max(Display.getInstance().getDisplayHeight(), 820);
            return new Dimension(w, h);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            int w = getWidth();
            int h = getHeight();

            g.setColor(0x0b1220);
            g.fillRect(0, 0, w, h);
            g.setColor(0x1e293b);
            g.drawRect(4, 4, w - 8, h - 8);

            drawShapesAndStrokes(g, w, h);
            drawColorAndAlpha(g, w, h);
            drawImagesAndText(g, w, h);
            drawTransforms(g, w, h);

            g.setColor(0xe2e8f0);
            g.drawString("Method under inspection:", 12, h - 52);
            g.setColor(0x22c55e);
            g.drawString(highlightedMethod, 12, h - 30);
        }

        private void drawShapesAndStrokes(Graphics g, int w, int h) {
            int areaW = (w - 36) / 2;
            int areaH = (h - 120) / 2;
            int x = 12;
            int y = 12;

            g.setColor(0x111827);
            g.fillRect(x, y, areaW, areaH);
            g.setColor(0x475569);
            g.drawRect(x, y, areaW - 1, areaH - 1);

            g.setColor(0xfacc15);
            g.drawLine(x + 10, y + 10, x + areaW - 10, y + 18);
            g.setColor(0x22c55e);
            g.fillRect(x + 10, y + 30, areaW / 3, 32);
            g.setColor(0xf97316);
            g.drawRect(x + areaW / 3 + 18, y + 30, areaW / 3, 32);

            g.setColor(0x38bdf8);
            g.fillRoundRect(x + 10, y + 70, areaW / 3, 40, 16, 16);
            g.setColor(0xec4899);
            g.drawRoundRect(x + areaW / 3 + 18, y + 70, areaW / 3, 40, 16, 16);

            g.setColor(0xa855f7);
            g.fillArc(x + 10, y + 118, 70, 70, 30, 210);
            g.setColor(0x6366f1);
            g.drawArc(x + areaW / 3 + 20, y + 118, 72, 72, 210, 280);

            int[] px = new int[] {x + areaW - 80, x + areaW - 34, x + areaW - 58};
            int[] py = new int[] {y + 30, y + 30, y + 76};
            g.setColor(0x7dd3fc);
            g.fillPolygon(px, py, px.length);
            g.setColor(0x0ea5e9);
            g.drawPolygon(px, py, px.length);

            g.setColor(0xe5e7eb);
            g.drawString("Strokes + primitives", x + 10, y + areaH - 18);
        }

        private void drawColorAndAlpha(Graphics g, int w, int h) {
            int areaW = (w - 36) / 2;
            int areaH = (h - 120) / 2;
            int x = (w + 12) / 2;
            int y = 12;

            g.setColor(0x0f172a);
            g.fillRect(x, y, areaW, areaH);
            g.setColor(0x475569);
            g.drawRect(x, y, areaW - 1, areaH - 1);

            g.fillLinearGradient(0x22d3ee, 0x2563eb, x + 10, y + 10, areaW - 20, 76, true);
            g.fillRadialGradient(0xf43f5e, 0xf59e0b, x + 12, y + 98, areaW / 2 - 16, areaH - 120);
            g.fillRectRadialGradient(0x10b981, 0x22c55e, x + areaW / 2, y + 98, areaW / 2 - 14, areaH - 120, 0.28f, 0.52f, 0.78f);

            int previousAlpha = g.getAlpha();
            g.setAlpha(150);
            g.setColor(0xfef3c7);
            g.fillRect(x + 14, y + 26, areaW / 2, 44);
            g.setAlpha(previousAlpha);

            g.setColor(0xe2e8f0);
            g.drawString("Gradients + alpha", x + 12, y + areaH - 18);
        }

        private void drawImagesAndText(Graphics g, int w, int h) {
            int areaW = (w - 36) / 2;
            int areaH = (h - 120) / 2;
            int x = 12;
            int y = (h + 24) / 2;

            g.setColor(0x0f172a);
            g.fillRect(x, y, areaW, areaH);
            g.setColor(0x334155);
            g.drawRect(x, y, areaW - 1, areaH - 1);

            g.setAntiAliasedText(true);
            g.setColor(0x22c55e);
            g.drawString("drawString", x + 10, y + 18);
            g.setColor(0xf97316);
            g.drawStringBaseline("baseline", x + 120, y + 18);

            g.setColor(0xf43f5e);
            g.drawChar('G', x + 10, y + 46);
            char[] chars = new char[] {'r', 'a', 'p', 'h', 'i', 'c', 's'};
            g.setColor(0xa855f7);
            g.drawChars(chars, 0, chars.length, x + 32, y + 46);

            Image sample = getCachedLogo();
            if (sample != null) {
                g.drawImage(sample, x + 10, y + 74);
                g.drawImage(sample, x + 84, y + 74, 52, 52);
            }

            g.setColor(0xe2e8f0);
            g.drawString("Text + images", x + 10, y + areaH - 18);
            g.setAntiAliasedText(false);
        }

        private void drawTransforms(Graphics g, int w, int h) {
            int areaW = (w - 36) / 2;
            int areaH = (h - 120) / 2;
            int x = (w + 12) / 2;
            int y = (h + 24) / 2;

            g.setColor(0x0f172a);
            g.fillRect(x, y, areaW, areaH);
            g.setColor(0x475569);
            g.drawRect(x, y, areaW - 1, areaH - 1);

            int cx = x + areaW / 2;
            int cy = y + areaH / 2;

            g.translate(cx, cy);
            g.setColor(0x22c55e);
            g.fillRect(-44, -18, 88, 36);

            if (g.isAffineSupported()) {
                g.rotateRadians((float) Math.toRadians(15));
                g.scale(0.9f, 0.9f);
                g.setColor(0xf97316);
                g.fillRoundRect(-58, -26, 116, 52, 14, 14);
                g.shear(0.15f, 0f);
                g.setColor(0x38bdf8);
                g.drawRect(-64, -30, 128, 60);
                g.resetAffine();
            }

            g.translate(-cx, -cy);
            g.setColor(0xe2e8f0);
            g.drawString("Transforms", x + 10, y + areaH - 18);
        }

        private Image getCachedLogo() {
            if (cachedLogo != null && !cachedLogo.isAnimation()) {
                return cachedLogo;
            }
            try {
                cachedLogo = Image.createImage(64, 64, 0xffe0f2fe);
                Graphics g = cachedLogo.getGraphics();
                g.setColor(0x1d4ed8);
                g.fillRect(4, 4, 56, 56);
                g.setColor(0xf97316);
                g.drawRect(2, 2, 60, 60);
                g.setColor(0xfef3c7);
                g.drawString("CN1", 10, 26);
            } catch (Exception ignored) {
                // Fallback to null if we cannot allocate the image.
            }
            return cachedLogo;
        }
    }
}

