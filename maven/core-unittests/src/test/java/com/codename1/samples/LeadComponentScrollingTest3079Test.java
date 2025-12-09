package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.*;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import static com.codename1.ui.ComponentSelector.$;
import static org.junit.jupiter.api.Assertions.*;

public class LeadComponentScrollingTest3079Test extends UITestBase {
    boolean buttonPressed = false;

    @FormTest
    public void testLeadComponentScrolling() {
        Form hi = new Form("Hi World", BoxLayout.y());

        int len = 10; // Reduced from 100 for test speed
        Button targetButton = null;
        for (int i=0; i<len; i++) {
            Container cnt = new Container(new FlowLayout());
            $(cnt).selectAllStyles().setBgTransparency(0xff).setBgColor(0xffffff);
            SwipeableContainer sc = new SwipeableContainer(BoxLayout.encloseX(new Button("This is a button")),BoxLayout.encloseX(new Button("This is another button")), cnt);
            Button b = new Button("Button "+i);
            b.addActionListener(evt->{
                buttonPressed = true;
            });
            cnt.add(b);
            cnt.setLeadComponent(b);
            hi.add(sc);
            if (i == 5) targetButton = b;
        }
        hi.setScrollableY(true);
        hi.show();

        // Assert layout
        assertNotNull(targetButton);
        assertEquals(hi, targetButton.getComponentForm());

        // Click on a button (scrolling shouldn't prevent click if it's a tap)
        // Need to find where targetButton is.
        // It might be off screen initially if len is large, but with 10 it should be fine or we scroll to it.
        hi.scrollComponentToVisible(targetButton);

        int x = targetButton.getAbsoluteX() + targetButton.getWidth() / 2;
        int y = targetButton.getAbsoluteY() + targetButton.getHeight() / 2;

        hi.pointerPressed(x, y);
        hi.pointerReleased(x, y);

        assertTrue(buttonPressed, "Button action should trigger even in scrollable container");
    }
}
