package com.codename1.ui.animations;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;

public class BubbleTransitionTest extends UITestBase {

    @FormTest
    public void testBubbleTransition() {
        Form f1 = new Form("Source") {
            @Override
            public int getWidth() { return 500; }
            @Override
            public int getHeight() { return 500; }
        };
        Label l1 = new Label("Source Label");
        f1.add(l1);
        f1.layoutContainer();

        Form f2 = new Form("Dest") {
            @Override
            public int getWidth() { return 500; }
            @Override
            public int getHeight() { return 500; }
        };
        Label l2 = new Label("Dest Label");
        f2.add(l2);
        f2.layoutContainer();

        // No shared component
        BubbleTransition t = new BubbleTransition(200, null);

        t.init(f1, f2);

        t.cleanup();
    }

    @FormTest
    public void testBubbleTransitionCopy() {
        BubbleTransition t = new BubbleTransition(200, "comp");
        t.setRoundBubble(false);
        BubbleTransition copy = (BubbleTransition) t.copy(false);
        Assertions.assertEquals(t.getDuration(), copy.getDuration());
    }
}
