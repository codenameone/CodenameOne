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
            g.pushClip();
            cleanPaint(g);
            g.popClip();
            g.setFont(font);
            g.setColor(color);
            g.setAlpha(alpha);
        }

        protected abstract void cleanPaint(Graphics g);
    }

    // The four rendering variants this test compares: direct vs mutable-image
    // painting, each with anti-aliasing off then on. On phone/tablet/desktop
    // all four are shown together in a 2x2 grid (one screenshot). On a watch
    // that grid is unreadably dense, so each variant is shown full-screen as a
    // separate screenshot (see runTest()).
    private static final String[] WATCH_VARIANT_SUFFIX = {
        "direct-aa-off", "direct-aa-on", "image-aa-off", "image-aa-on"
    };

    private CleanPaintComponent variant(final int idx) {
        switch (idx) {
            case 0:
                return new CleanPaintComponent() {
                    @Override
                    public void cleanPaint(Graphics g) {
                        currentColor = -1;
                        g.setAntiAliased(false);
                        g.setAntiAliasedText(false);
                        g.fillRect(getX(), getY(), getWidth(), getHeight());
                        drawContent(g, getBounds());
                    }
                };
            case 1:
                return new CleanPaintComponent() {
                    @Override
                    public void cleanPaint(Graphics g) {
                        currentColor = -1;
                        g.setAntiAliased(true);
                        g.setAntiAliasedText(true);
                        g.fillRect(getX(), getY(), getWidth(), getHeight());
                        drawContent(g, getBounds());
                    }
                };
            case 2:
                return new CleanPaintComponent() {
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
                };
            default:
                return new CleanPaintComponent() {
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
                };
        }
    }

    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            // Watch form factor: four separate full-screen captures instead of a
            // cramped 2x2 grid. This exercises Display.isWatch()/getFormFactor()
            // and demonstrates a form-factor-appropriate test layout.
            showWatchVariant(0);
            return true;
        }
        Form form = createForm(screenshotName(), new GridLayout(2, 2), screenshotName());
        form.setUIID("GraphicsForm");
        for (int i = 0; i < 4; i++) {
            form.add(variant(i));
        }
        form.show();
        return true;
    }

    private void showWatchVariant(final int idx) {
        final String name = screenshotName() + "-" + WATCH_VARIANT_SUFFIX[idx];
        Form form = new Form(name, new BorderLayout()) {
            @Override
            protected void onShowCompleted() {
                registerReadyCallback(this, () -> captureWhenSettled(this, name, () -> {
                    if (idx + 1 < WATCH_VARIANT_SUFFIX.length) {
                        showWatchVariant(idx + 1);
                    } else {
                        done();
                    }
                }));
            }
        };
        form.setUIID("GraphicsForm");
        form.add(BorderLayout.CENTER, variant(idx));
        form.show();
    }
}
