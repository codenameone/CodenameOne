package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.testing.AbstractTest;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class GraphicsMethodsScreenshotTest extends AbstractTest {
    @Override
    public boolean runTest() throws Exception {
        List<String> methods = collectGraphicsMethods();
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

        AtomicBoolean success = new AtomicBoolean(true);
        int index = 1;
        for (String descriptor : methods) {
            final String label = descriptor + " (" + index + "/" + methods.size() + ")";
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
                success.set(false);
            }
            index++;
        }

        return success.get();
    }

    private static List<String> collectGraphicsMethods() {
        return Arrays.stream(Graphics.class.getDeclaredMethods())
                .filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
                .filter(m -> !m.isSynthetic())
                .sorted(Comparator.comparing(java.lang.reflect.Method::getName)
                        .thenComparing(m -> m.getParameterTypes().length)
                        .thenComparing(GraphicsMethodsScreenshotTest::parameterDescriptor))
                .map(GraphicsMethodsScreenshotTest::describeMethod)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static String describeMethod(java.lang.reflect.Method method) {
        StringBuilder descriptor = new StringBuilder();
        descriptor.append(method.getName());
        descriptor.append('(');
        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            descriptor.append(params[i].getSimpleName());
            if (i < params.length - 1) {
                descriptor.append(',');
            }
        }
        descriptor.append(')');
        return descriptor.toString();
    }

    private static String parameterDescriptor(java.lang.reflect.Method method) {
        StringBuilder descriptor = new StringBuilder();
        Class<?>[] params = method.getParameterTypes();
        for (Class<?> param : params) {
            descriptor.append(param.getSimpleName()).append('-');
        }
        return descriptor.toString();
    }

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

