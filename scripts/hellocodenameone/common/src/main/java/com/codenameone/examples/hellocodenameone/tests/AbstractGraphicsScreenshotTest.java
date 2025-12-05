package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;

public abstract class AbstractGraphicsScreenshotTest extends BaseTest {
    private final int[] colorSet = {0xff0000, 0xff00, 0xff, 0xffffff};
    private int currentColor = -1;

    protected void nextColor(Graphics g) {
        if (currentColor == -1) {
            currentColor = 0;
            g.setColor(colorSet[0]);
        }
        g.darkerColor(1);
        if (g.getColor() == 0) {
            currentColor++;
            if (currentColor == colorSet.length) {
                currentColor = 0;
            }
            g.setColor(colorSet[currentColor]);
        }
    }

    protected abstract void drawContent(Graphics g, Rectangle bounds);

    protected abstract String screenshotName();

    static abstract class CleanPaintComponent extends Component {
        CleanPaintComponent() {
            setUIID("GraphicsComponent");
        }

        @Override
        public void paint(Graphics g) {
            int alpha = g.getAlpha();
            int color = g.getColor();
            Font font = g.getFont();
            g.pushClip();
            cleanPaint(g);
            g.popClip();
            g.setFont(font);
            g.setColor(color);
            g.setAlpha(alpha);
        }

        protected abstract void cleanPaint(Graphics g);
    }

    @Override
    public boolean runTest() {
        Form form = createForm(screenshotName(), new GridLayout(2, 2), screenshotName());
        form.setUIID("GraphicsForm");
        form.add(new CleanPaintComponent() {
            @Override
            public void cleanPaint(Graphics g) {
                currentColor = -1;
                g.setAntiAliased(false);
                g.setAntiAliasedText(false);
                g.fillRect(getX(), getY(), getWidth(), getHeight());
                drawContent(g, getBounds());
            }
        });
        form.add(new CleanPaintComponent() {
            @Override
            public void cleanPaint(Graphics g) {
                currentColor = -1;
                g.setAntiAliased(true);
                g.setAntiAliasedText(true);
                g.fillRect(getX(), getY(), getWidth(), getHeight());
                drawContent(g, getBounds());
            }
        });
        form.add(new CleanPaintComponent() {
            private Image img;
            @Override
            public void cleanPaint(Graphics g) {
                if (img == null || img.getWidth() != getWidth() || img.getHeight() != getHeight()) {
                    currentColor = -1;
                    img = Image.createImage(getWidth(), getHeight());
                    Graphics imgGraphics = img.getGraphics();
                    imgGraphics.setAntiAliased(false);
                    imgGraphics.setAntiAliasedText(false);
                    drawContent(imgGraphics, new Rectangle(0, 0, img.getWidth(), img.getHeight()));
                }
                g.drawImage(img, getX(), getY());
            }
        });
        form.add(new CleanPaintComponent() {
            private Image img;
            @Override
            public void cleanPaint(Graphics g) {
                if (img == null || img.getWidth() != getWidth() || img.getHeight() != getHeight()) {
                    currentColor = -1;
                    img = Image.createImage(getWidth(), getHeight());
                    Graphics imgGraphics = img.getGraphics();
                    imgGraphics.setAntiAliased(true);
                    imgGraphics.setAntiAliasedText(true);
                    drawContent(imgGraphics, new Rectangle(0, 0, img.getWidth(), img.getHeight()));
                }
                g.drawImage(img, getX(), getY());
            }
        });

        form.show();
        return true;
    }
}
