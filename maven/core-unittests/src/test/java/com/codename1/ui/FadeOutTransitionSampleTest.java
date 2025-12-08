package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;

import static com.codename1.ui.ComponentSelector.$;
import static org.junit.jupiter.api.Assertions.*;

public class FadeOutTransitionSampleTest extends UITestBase {

    @FormTest
    public void testFadeOutTransition() {
        Form hi = new Form("Hi World", new BorderLayout());
        Button button1 = new Button("Button 1");
        $(button1).selectAllStyles().setBgColor(0xff0000);

        Button button2 = new Button("Button 2");
        $(button2).selectAllStyles().setBgColor(0x00ff00);

        Button doFade = new Button("Toggle");
        doFade.addActionListener(evt -> {
            Container contentPane = hi.getContentPane();
            if (contentPane.contains(button1)) {
                button2.remove();
                Container wrapper = BorderLayout.center(button2);

                contentPane.replace(button1.getParent(), wrapper, null);
            } else if (contentPane.contains(button2)) {
                Container empty = new Container();
                $(empty).selectAllStyles().setBgColor(0xeaeaea).setBgTransparency(0xff);

                contentPane.replace(button2.getParent(), empty, null);

            } else {
                Container empty = new Container();
                button1.remove();
                contentPane.add(BorderLayout.CENTER, empty);
                contentPane.revalidateWithAnimationSafety();
                contentPane.replace(empty, BorderLayout.center(button1), null);
            }
        });

        hi.add(BorderLayout.NORTH, doFade);
        hi.show();
        DisplayTest.flushEdt();

        Container contentPane = hi.getContentPane();
        assertFalse(contentPane.contains(button1), "Button 1 should not be present initially");
        assertFalse(contentPane.contains(button2), "Button 2 should not be present initially");

        // 1. Click Toggle -> Button 1 added
        implementation.tapComponent(doFade);
        DisplayTest.flushEdt();
        assertTrue(contentPane.contains(button1), "Button 1 should be present after 1st click");
        assertFalse(contentPane.contains(button2), "Button 2 should not be present after 1st click");

        // 2. Click Toggle -> Button 2 replaces Button 1
        implementation.tapComponent(doFade);
        DisplayTest.flushEdt();
        assertFalse(contentPane.contains(button1), "Button 1 should not be present after 2nd click");
        assertTrue(contentPane.contains(button2), "Button 2 should be present after 2nd click");

        // 3. Click Toggle -> Empty replaces Button 2
        implementation.tapComponent(doFade);
        DisplayTest.flushEdt();
        assertFalse(contentPane.contains(button1), "Button 1 should not be present after 3rd click");
        assertFalse(contentPane.contains(button2), "Button 2 should not be present after 3rd click");

        // 4. Click Toggle -> Button 1 added again
        implementation.tapComponent(doFade);
        DisplayTest.flushEdt();
        assertTrue(contentPane.contains(button1), "Button 1 should be present after 4th click");
    }
}
