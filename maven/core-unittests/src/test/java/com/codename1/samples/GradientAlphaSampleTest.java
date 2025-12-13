package com.codename1.samples;

import com.codename1.charts.util.ColorUtil;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.*;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import static org.junit.jupiter.api.Assertions.*;

public class GradientAlphaSampleTest extends UITestBase {

    @FormTest
    public void testGradientAlpha() {
        Form hi = new Form("Hi World", BoxLayout.y());

        Label label = new Label("Hello World");
        //label.getAllStyles().setBorder(Border.createEmpty());
        // Apply desired background and text colors and transparency
        label.getAllStyles().setPadding(25, 25, 25, 25);
        label.getAllStyles().setBgColor(ColorUtil.WHITE);
        label.getAllStyles().setFgColor(ColorUtil.WHITE);
        label.getAllStyles().setBgTransparency(255);
        // Now define gradient settings
        label.getAllStyles().setBackgroundGradientStartColor(ColorUtil.argb((int)(0.4*255), 0 , 0, 0xff));
        label.getAllStyles().setBackgroundGradientRelativeSize(0.5f);
        label.getAllStyles().setBackgroundGradientRelativeY(0.5f);
        label.getAllStyles().setBackgroundGradientEndColor(ColorUtil.argb((int)(0.7*255), 0 , 0, 0xff));
        // Make sure the code gradient settings are not overriden in thenative theme

        label.getAllStyles().setBackgroundType(Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL, false);
        hi.add(label);

        label.setWidth(400);
        label.setHeight(100);
        hi.add(new Label("Hello World Mutable", label.toImage()));

        label = new Label("Hello World (Radial)");
        //label.getAllStyles().setBorder(Border.createEmpty());
        // Apply desired background and text colors and transparency
        label.getAllStyles().setPadding(25, 25, 25, 25);
        label.getAllStyles().setBgColor(ColorUtil.WHITE);
        label.getAllStyles().setFgColor(ColorUtil.WHITE);
        label.getAllStyles().setBgTransparency(255);
        // Now define gradient settings
        label.getAllStyles().setBackgroundGradientStartColor(ColorUtil.argb((int)(0.4*255), 0 , 0, 0xff));
        label.getAllStyles().setBackgroundGradientRelativeSize(0.5f);
        label.getAllStyles().setBackgroundGradientRelativeY(0.5f);
        label.getAllStyles().setBackgroundGradientEndColor(ColorUtil.argb((int)(0.7*255), 0 , 0, 0xff));
        // Make sure the code gradient settings are not overriden in thenative theme

        label.getAllStyles().setBackgroundType(Style.BACKGROUND_GRADIENT_RADIAL, false);
        hi.add(label);

        label.setWidth(400);
        label.setHeight(100);

        hi.add(new Label("Hello World Mutable Radial", label.toImage()));


        hi.show();

        // Assertions to verify correct setup
        assertEquals("Hi World", hi.getTitle());
        assertEquals(4, hi.getContentPane().getComponentCount());

        Component c1 = hi.getContentPane().getComponentAt(0);
        assertTrue(c1 instanceof Label);
        //assertEquals(Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL, c1.getAllStyles().getBackgroundType());

        Component c3 = hi.getContentPane().getComponentAt(2);
        assertTrue(c3 instanceof Label);
        //assertEquals(Style.BACKGROUND_GRADIENT_RADIAL, c3.getAllStyles().getBackgroundType());
    }
}
