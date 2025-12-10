package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Stroke;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.Painter;
import org.junit.jupiter.api.Assertions;

public class ShapeClipTestTest extends UITestBase {

    @FormTest
    public void testShapeClip() {
        Form hi = new Form("Shape Clip");

        // We create a 50 x 100 shape
        GeneralPath path = new GeneralPath();
        path.moveTo(20, 0);
        path.lineTo(30, 0);
        path.lineTo(30, 100);
        path.lineTo(20, 100);
        path.lineTo(20, 15);
        path.lineTo(5, 40);
        path.lineTo(5, 25);
        path.lineTo(20, 0);

        Stroke stroke = new Stroke(0.5f, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 4);

        Painter p = (Graphics g, Rectangle rect) -> {
            g.setColor(0xff);
            float widthRatio = ((float) rect.getWidth()) / 50f;
            float heightRatio = ((float) rect.getHeight()) / 100f;
            g.scale(widthRatio, heightRatio);
            g.translate((int) (((float) rect.getX()) / widthRatio), (int) (((float) rect.getY()) / heightRatio));
            Transform t = g.getTransform();
            t.rotate((float)Math.PI/4, rect.getWidth()/2/widthRatio, rect.getHeight()/2/heightRatio);
            g.setTransform(t);
            g.setClip(path);
            g.setAntiAliased(true);
            g.setColor(0x00ff00);
            g.fillRect(0, 0, 50, 100);
            g.setClip(path.getBounds());
            g.drawShape(path, stroke);
            g.translate(-(int) (((float) rect.getX()) / widthRatio), -(int) (((float) rect.getY()) / heightRatio));
            g.resetAffine();
        };

        hi.getContentPane().getUnselectedStyle().setBgPainter(p);

        hi.show();

        implementation.setShapeSupported(true);
        implementation.resetShapeTracking();

        Image img = Image.createImage(100, 200);
        Graphics g = img.getGraphics();

        Rectangle rect = new Rectangle(0, 0, 100, 200);
        p.paint(g, rect);

        Assertions.assertTrue(implementation.wasDrawShapeInvoked(), "drawShape should have been invoked");
        Assertions.assertTrue(implementation.getLastClipShape() != null || implementation.getClipX(null) != 0, "Clip should have been set (either shape or rect)");
    }
}
