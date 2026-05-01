package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CN;
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
    private int factor = 0;

    protected void nextColor(Graphics g) {
        if(factor == 0) {
            factor = CN.getDisplayWidth() < 400 ? 5 : 1;
        }
        if(currentColor == -1) {
            currentColor = 0;
            g.setColor(colorSet[0]);
        }
        g.darkerColor(1);
        if(g.getColor() == 0) {
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
            // Re-sync the native graphics' font with the Java-side current
            // font: Label.paint -> Display.impl.drawLabelComponent calls
            // setNativeFont() to push the label's style font into the
            // platform NativeGraphics, but does NOT update Graphics.current.
            // The next g.drawString() call therefore picks up the LABEL's
            // leftover font (e.g. the form's title font on iOS, where
            // impl.drawString reads ng.getFont() instead of the
            // Java-current-derived nativeFont parameter). Without this
            // re-sync the first test panel renders strings in the title
            // font while subsequent panels (after this paint's restoring
            // g.setFont(font) tail) come out correctly. See the iOS Metal
            // port investigation around CN1MetalDrawString.
            g.setFont(font);
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

        configureForm(form);
        form.show();
        return true;
    }

    /// Hook for subclasses to tweak the form before show(). Heavy rendering
    /// tests (e.g. FillRoundRect / DrawRoundRect on the Metal port) override
    /// this to disable the slide-and-fade transition, which on Metal can leave
    /// residual title pixels in the persistent screenTexture under heavy paint
    /// load.
    protected void configureForm(com.codename1.ui.Form form) {
    }
}
