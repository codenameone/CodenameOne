package com.codename1.samples;

import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.CN;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import static org.junit.jupiter.api.Assertions.*;

public class RoundRectBorderTestTest extends UITestBase {

    @FormTest
    public void testRoundRectBorder() {
        Form f = new Form("Hi World", BoxLayout.y());
        f.add(new Label("Hi World"));
        Label l = l("Rectangle");
        l.getStyle().setBorder(b().cornerRadius(0f));
        f.add(l);
        l = l("All corners");
        l.getStyle().setBorder(b());
        f.add(l);
        l = l("Top Only");
        l.getStyle().setBorder(b().topOnlyMode(true));
        f.add(l);
        l = l("Bottom Only");

        l.getStyle().setBorder(b().bottomOnlyMode(true));
        f.add(l);
        l = l("Top Left Only");
        l.getStyle().setBorder(b().topLeftMode(true).topRightMode(false).bottomLeftMode(false).bottomRightMode(false));
        f.add(l);
        l = l("Top Right Only");
        l.getStyle().setBorder(b().topLeftMode(false).topRightMode(true).bottomLeftMode(false).bottomRightMode(false));
        f.add(l);
        l = l("Bottom Left Only");
        l.getStyle().setBorder(b().topLeftMode(false).topRightMode(false).bottomLeftMode(true).bottomRightMode(false));
        f.add(l);
        l = l("Bottom Right Only");
        l.getStyle().setBorder(b().topLeftMode(false).topRightMode(false).bottomLeftMode(false).bottomRightMode(true));
        f.add(l);
        l = l("Left Only");
        l.getStyle().setBorder(b().topLeftMode(true).topRightMode(false).bottomLeftMode(true).bottomRightMode(false));
        f.add(l);
        l = l("Right Only");
        l.getStyle().setBorder(b().topLeftMode(false).topRightMode(true).bottomLeftMode(false).bottomRightMode(true));
        f.add(l);

        l = l("Border Radius 10");
        l.getStyle().setBorder(b().cornerRadius(10f));


        f.add(l);

        l = l("Bottom left and right");
        l.getStyle().setBorder(b().bottomLeftMode(true).bottomRightMode(true).topLeftMode(false).topRightMode(false));
        f.add(l);

        f.show();

        // Basic assertions
        assertEquals(13, f.getContentPane().getComponentCount());

        // We could assert border properties for some components
        Label rect = (Label) f.getContentPane().getComponentAt(1);
        RoundRectBorder rb = (RoundRectBorder) rect.getStyle().getBorder();
        assertEquals(0f, rb.getCornerRadius(), 0.001);

        Label topOnly = (Label) f.getContentPane().getComponentAt(3);
        rb = (RoundRectBorder) topOnly.getStyle().getBorder();
        assertTrue(rb.isTopOnlyMode());
    }

    private RoundRectBorder b() {
        RoundRectBorder out = RoundRectBorder.create();
        out.cornerRadius(5f);
        return out;
    }

    private Label l(String text) {
        Label l = new Label(text);
        int p = CN.convertToPixels(5);
        l.getStyle().setPadding(p, p, p, p);
        l.getStyle().setBgColor(0x333333);
        l.getStyle().setFgColor(0xffffff);
        l.getStyle().setBgTransparency(0xff);
        return l;
    }
}
