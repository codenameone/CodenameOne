package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.paint.LinearGradientPaint;

public class GraphicsStateAndTextScreenshotTest extends AbstractGraphicsScreenshotTest {
    @Override
    protected Component createContent() {
        return new StateAndTextCanvas();
    }

    @Override
    protected String screenshotName() {
        return "GraphicsStateAndText";
    }

    private static final class StateAndTextCanvas extends Component {
        @Override
        protected Dimension calcPreferredSize() {
            int w = Math.max(Display.getInstance().getDisplayWidth(), 480);
            int h = Math.max(Display.getInstance().getDisplayHeight(), 720);
            return new Dimension(w, h);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(0x0f172a);
            g.fillRect(0, 0, w, h);

            int margin = 12;
            int columnWidth = (w - margin * 3) / 2;
            int rowHeight = (h - margin * 3) / 2;

            paintColorAndClips(g, margin, margin, columnWidth, rowHeight);
            paintAlphaAndRendering(g, margin * 2 + columnWidth, margin, columnWidth, rowHeight);
            paintTextAndFonts(g, margin, margin * 2 + rowHeight, columnWidth * 2 + margin, rowHeight);
        }

        private void paintColorAndClips(Graphics g, int x, int y, int w, int h) {
            int originalColor = g.setAndGetColor(0x1d4ed8);
            g.fillRect(x, y, w, h);
            g.setColor(0x334155);
            g.drawRect(x, y, w - 1, h - 1);

            g.lighterColor(40);
            g.fillRect(x + 8, y + 8, w / 3, 24);
            g.darkerColor(80);
            g.fillRect(x + 8, y + 40, w / 3, 24);

            LinearGradientPaint paint = new LinearGradientPaint(0, 0, w, 0, new float[] {0f, 1f}, new int[] {0xff22d3ee, 0xff6366f1});
            g.setColor(paint);
            GeneralPath shape = new GeneralPath();
            shape.moveTo(x + w - 12, y + 12);
            shape.lineTo(x + w / 2f, y + h / 2f);
            shape.lineTo(x + w - 12, y + h - 12);
            shape.lineTo(x + w - 12, y + 12);
            g.fillShape(shape);
            g.setColor(g.getColor());
            g.drawString("paint fillShape", x + w / 2 - 40, y + h - 24);

            int[] clipArray = new int[] {x + w / 6, y + h / 4, w / 2, h / 3};
            g.setClip(clipArray);
            int[] fullClip = g.getClip();
            int clipX = g.getClipX();
            int clipY = g.getClipY();
            int clipW = g.getClipWidth();
            int clipH = g.getClipHeight();
            g.pushClip();
            g.clipRect(x + w / 4, y + h / 3, w / 2, h / 2);
            g.setColor(0xfbbf24);
            g.fillRect(x, y, w, h);
            g.popClip();
            GeneralPath clipShape = new GeneralPath();
            clipShape.moveTo(x + 20, y + 20);
            clipShape.lineTo(x + 50, y + 20);
            clipShape.lineTo(x + 20, y + 60);
            clipShape.closePath();
            if (g.isShapeClipSupported()) {
                g.setClip(clipShape);
                g.setColor(0x10b981);
                g.fillRect(x, y, w, h);
            }
            g.setClip(new Rectangle(clipX, clipY, clipW, clipH));
            g.setClip(x, y, w, h);
            g.setColor(0xe2e8f0);
            g.drawString("clip info: " + clipX + "," + clipY + " " + clipW + "x" + clipH, x + 10, y + 14);
            g.drawString("clip array=" + fullClip[0] + "," + fullClip[1], x + 10, y + 34);
            g.setColor(originalColor);
        }

        private void paintAlphaAndRendering(Graphics g, int x, int y, int w, int h) {
            g.setColor(0x0f766e);
            g.fillRect(x, y, w, h);
            g.setColor(0x14b8a6);
            g.drawRect(x, y, w - 1, h - 1);

            boolean alphaSupported = g.isAlphaSupported();
            boolean aaSupported = g.isAntiAliasingSupported();
            boolean aaTextSupported = g.isAntiAliasedTextSupported();
            int oldAlpha = g.getAlpha();
            int previousAlpha = g.setAndGetAlpha(180);
            g.setColor(0xf43f5e);
            g.fillRect(x + 8, y + 8, w / 2, 40);
            int concatenatedAlpha = g.concatenateAlpha(200);
            g.setColor(0xfcd34d);
            g.fillRect(x + 16, y + 28, w / 2, 40);
            g.setAlpha(previousAlpha);
            g.setAlpha(oldAlpha);

            g.setRenderingHints(Graphics.RENDERING_HINT_FAST);
            int hints = g.getRenderingHints();
            g.setRenderingHints(0);

            g.setColor(0x0ea5e9);
            g.fillRect(x + w / 2 + 8, y + 12, w / 3, 28);
            g.fillRect(x + w / 2 + 8, y + 48, w / 3, 28, (byte) 120);
            g.clearRect(x + w / 2 + 8, y + 84, w / 3, 18);

            g.setAntiAliased(true);
            g.setAntiAliasedText(true);
            boolean aa = g.isAntiAliased();
            boolean aaText = g.isAntiAliasedText();
            g.setColor(0xf8fafc);
            g.drawString("alpha supported=" + alphaSupported + " aa sup=" + aaSupported + " text sup=" + aaTextSupported, x + 8, y + h - 56);
            g.drawString("aa=" + aa + " text=" + aaText + " hints=" + hints + " concat=" + concatenatedAlpha, x + 8, y + h - 36);
            g.setAntiAliased(false);
            g.setAntiAliasedText(false);
        }

        private void paintTextAndFonts(Graphics g, int x, int y, int w, int h) {
            g.setClip(x, y, w, h);
            g.setColor(0x111827);
            g.fillRect(x, y, w, h);
            g.setColor(0x475569);
            g.drawRect(x, y, w - 1, h - 1);

            Font original = g.getFont();
            g.setFont(original.derive(Font.STYLE_BOLD, original.getHeight() + 2));
            g.setColor(0x22c55e);
            g.drawString("drawString", x + 10, y + 10, Style.TEXT_DECORATION_UNDERLINE);
            g.setFont(original);

            g.setColor(0xf97316);
            g.drawStringBaseline("baseline", x + 10, y + 42);
            g.setColor(0x38bdf8);
            g.drawStringBaseline("decorated", x + 120, y + 42, Style.TEXT_DECORATION_STRIKETHRU);

            g.setColor(0xf43f5e);
            g.drawChar('G', x + 10, y + 72);
            char[] chars = new char[] {'r', 'a', 'p', 'h', 'i', 'c', 's'};
            g.setColor(0xa855f7);
            g.drawChars(chars, 0, chars.length, x + 32, y + 72);

            Image img = Image.createImage(60, 60, 0xffe0f2fe);
            Graphics imgG = img.getGraphics();
            imgG.setColor(0x1d4ed8);
            imgG.fillRect(4, 4, 52, 52);
            g.drawImage(img, x + 10, y + 104);
            g.drawImage(img, x + 84, y + 104, 48, 48);

            g.setColor(0xe5e7eb);
            g.drawString("translate=" + g.getTranslateX() + "," + g.getTranslateY(), x + 10, y + h - 38);
            g.drawString("paint? " + (g.getPaint() != null), x + 10, y + h - 20);
            g.setClip(0, 0, getWidth(), getHeight());
        }
    }
}
