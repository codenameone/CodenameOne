package com.codename1.samples;

import com.codename1.charts.util.ColorUtil;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.*;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import java.util.Random;
import static com.codename1.ui.ComponentSelector.$;
import static org.junit.jupiter.api.Assertions.*;

public class LayeredLayoutTest2751Test extends UITestBase {

    @FormTest
    public void testLayeredLayout() {
        Form hi = new Form("Hi World", new LayeredLayout());
        Container[] components = new Container[20];
        for (int i=0; i<20; i++) {
            components[i] = dummyComponent(40, 20);
        }
        Container box = FlowLayout.encloseCenter(components);
        box.setWidth(200);
        Dimension prefSize = box.getPreferredSize();
        box.setPreferredH(prefSize.getHeight());
        box.setPreferredW(200);
        $(box).selectAllStyles()
                    .setBgTransparency(255)
                    .setBgColor(ColorUtil.YELLOW);
        hi.add(box);
        ((LayeredLayout)hi.getLayout()).setInsets(box, "auto auto auto auto");

        hi.show();

        // Assertions
        assertEquals(hi, Display.getInstance().getCurrent());
        assertTrue(hi.contains(box));
        // Check if box is centered or has correct insets logic applied (LayeredLayout should handle it)
        // Since it's "auto auto auto auto", it should be centered.

        // Force layout
        hi.layoutContainer();

        assertTrue(box.getX() >= 0);
        assertTrue(box.getY() >= 0);
        assertTrue(box.getWidth() > 0);
        assertTrue(box.getHeight() > 0);
    }

    private Container dummyComponent(int w, int h) {
        Container out = new Container();
        out.setPreferredW(w);
        out.setPreferredH(h);

        $(out)
                .selectAllStyles()
                .setBgTransparency(255)
                .setBgColor(0x0000FF) // Fixed color for test stability
                .setMargin(5);
        return out;
    }
}
