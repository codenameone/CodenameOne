package com.codename1.samples;

import com.codename1.ui.*;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;

import static org.junit.jupiter.api.Assertions.*;

public class ZoomOutImageSampleTest extends UITestBase {

    private ImageCmp lbl;
    private Form hi;

    @FormTest
    public void testZoomOutImageSample() {
        hi = new Form("Hi World", new BorderLayout());
        Button zoomIn = new Button("Zoom In");

        Button zoomOut = new Button("Zoom Out");

        // Setup UI
        // Mock image download
        Image img = Image.createImage(100, 100, 0xff0000);
        lbl = new ImageCmp(img);
        Container cnt = new Container(new ZoomLayout());
        cnt.add(lbl);
        hi.add(BorderLayout.CENTER, cnt);
        hi.add(BorderLayout.NORTH, GridLayout.encloseIn(2, zoomIn, zoomOut));
        hi.show();

        waitForForm(hi);

        // Logic for buttons
        zoomIn.addActionListener(evt -> {
            if (lbl == null) return;
            lbl.setScale(10.0);
            lbl.getParent().animateLayout(100); // 100ms for test
        });

        zoomOut.addActionListener(evt -> {
            if (lbl == null) return;
            lbl.setScale(0.1);
            lbl.getParent().animateLayout(100); // 100ms for test
        });

        // Verification

        // Initial state
        assertEquals(1.0, lbl.scale, 0.001, "Initial scale should be 1.0");

        // Zoom In
        clickButton(zoomIn);
        waitForAnimation(300);
        assertEquals(10.0, lbl.scale, 0.001, "Scale should be 10.0 after zooming in");

        // Zoom Out
        clickButton(zoomOut);
        waitForAnimation(300);
        assertEquals(0.1, lbl.scale, 0.001, "Scale should be 0.1 after zooming out");
    }

    private void waitForForm(Form form) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 3000) {
            if (Display.getInstance().getCurrent() == form) {
                return;
            }
            try {
                Thread.sleep(20);
                com.codename1.ui.DisplayTest.flushEdt();
            } catch (InterruptedException e) {
            }
        }
    }

    private void clickButton(Button b) {
        // Use public method from TestCodenameOneImplementation
        implementation.dispatchPointerPress(b.getAbsoluteX() + b.getWidth() / 2, b.getAbsoluteY() + b.getHeight() / 2);
        implementation.dispatchPointerRelease(b.getAbsoluteX() + b.getWidth() / 2, b.getAbsoluteY() + b.getHeight() / 2);
        com.codename1.ui.DisplayTest.flushEdt();
    }

    private void waitForAnimation(int timeout) {
         long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
             com.codename1.ui.DisplayTest.flushEdt();
             try {
                Thread.sleep(50);
             } catch (InterruptedException e) {}
        }
    }

    static class ImageCmp extends Component {
        private Image img;
        private double scale = 1.0;

        ImageCmp(Image img) {
            this.img = img;
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension((int)(img.getWidth()*scale), (int)(img.getHeight()*scale));
        }

        void setScale(double scale) {
            this.scale = scale;
            setShouldCalcPreferredSize(true);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            g.drawImage(img, getX(), getY(), getWidth(), getHeight());
        }
    }

    static class ZoomLayout extends Layout {
        @Override
        public void layoutContainer(Container cnt) {
            for (Component cmp : cnt) {
                cmp.setX((cnt.getLayoutWidth() - cmp.getPreferredW())/2);
                cmp.setY((cnt.getLayoutHeight() - cmp.getPreferredH())/2);
                cmp.setWidth(cmp.getPreferredW());
                cmp.setHeight(cmp.getPreferredH());
            }
        }

        @Override
        public Dimension getPreferredSize(Container cnt) {
            Dimension dim = new Dimension();
            for (Component cmp : cnt) {
                dim.setWidth(Math.max(cmp.getPreferredW(), dim.getWidth()));
                dim.setHeight(Math.max(cmp.getPreferredH(), dim.getHeight()));
            }
            return dim;
        }
    }
}
