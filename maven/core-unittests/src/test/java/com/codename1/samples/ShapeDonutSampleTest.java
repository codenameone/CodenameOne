package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Painter;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BoxLayout;
import org.junit.jupiter.api.Assertions;

public class ShapeDonutSampleTest extends UITestBase {

    @FormTest
    public void testShapeDonut() {
        Form hi = new Form("Hi World", BoxLayout.y());
        Painter p = new Painter() {
            @Override
            public void paint(Graphics g, Rectangle rect) {
                GeneralPath p = new GeneralPath();

                p.setRect(new Rectangle(rect.getX()+10, rect.getY()+10, rect.getWidth()-20, rect.getHeight()-20), null);

                // Since default winding rule is EVEN_ODD, when we fill a shape with a closed circle inside a
                // rectangle, it will result in the circle being subtracted from the rect.
                p.arc(rect.getX()+30, rect.getY()+30, rect.getWidth()-60, rect.getHeight()-60, 0, Math.PI*2);
                g.setColor(0x0000ff);

                g.fillShape(p);
            }
        };
        hi.getContentPane().getStyle().setBgPainter(p);
        hi.show();

        implementation.setShapeSupported(true);
        implementation.resetShapeTracking();

        Image img = Image.createImage(200, 200);
        Graphics g = img.getGraphics();

        Rectangle rect = new Rectangle(0, 0, 200, 200);
        p.paint(g, rect);

        Assertions.assertTrue(implementation.wasFillShapeInvoked(), "fillShape should have been invoked");
        Assertions.assertNotNull(implementation.getLastFillShape(), "Last fill shape should not be null");
    }
}
