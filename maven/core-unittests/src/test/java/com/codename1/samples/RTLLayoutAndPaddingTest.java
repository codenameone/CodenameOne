package com.codename1.samples;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.RoundBorder;
import com.codename1.ui.plaf.UIManager;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.AutoCompleteTextField;
import static com.codename1.ui.CN.*;
import static org.junit.jupiter.api.Assertions.*;

public class RTLLayoutAndPaddingTest extends UITestBase {

    @FormTest
    public void testRTLLayoutAndPadding() {
        Form f = new Form("Hi World", BoxLayout.y());
        UIManager.getInstance().getLookAndFeel().setRTL(true);
        f.add(new Label("Hi World"));
        FlowLayout fl = new FlowLayout();
        fl.setAlign(CENTER);

        Container row = new Container(fl);
        row.add(new Label("Center"));
        $(row).selectAllStyles()
                .setPaddingMillimeters(3f)
                .setBgColor(0x003366).setBorder(RoundBorder.create());
        f.add(row);
        fl = new FlowLayout();
        fl.setAlign(RIGHT);
        row = new Container(fl);
        row.add(new Label("Right"));
        $(row).selectAllStyles()
                .setPaddingMillimeters(3f)
                .setBgColor(0x003366).setBorder(RoundBorder.create());
        f.add(row);
        fl = new FlowLayout();
        fl.setAlign(LEFT);
        row = new Container(fl);
        row.add(new Label("Left"));
        $(row).selectAllStyles()
                .setPaddingMillimeters(3f)
                .setBgColor(0x003366).setBorder(RoundBorder.create());
        f.add(row);

        fl = new FlowLayout();
        fl.setAlign(CENTER);
        row = new Container(fl);
        row.add(new Label("Center"));
        $(row).selectAllStyles()
                .setPaddingMillimeters(1f, 2f, 1f, 5f)

                .setBgColor(0x003366).setBorder(RoundBorder.create());
        f.add(row);

        AutoCompleteTextField tf = new AutoCompleteTextField("Red", "Green", "Blue");
        f.add(FlowLayout.encloseIn(tf));
        f.show();

        // Assert that RTL is set
        assertTrue(UIManager.getInstance().getLookAndFeel().isRTL());

        // Check component count in content pane
        assertEquals(6, f.getContentPane().getComponentCount());

        // Cleanup
        UIManager.getInstance().getLookAndFeel().setRTL(false);
    }
}
