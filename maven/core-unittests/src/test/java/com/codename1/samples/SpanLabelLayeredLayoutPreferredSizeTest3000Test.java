package com.codename1.samples;

import com.codename1.components.SpanLabel;
import com.codename1.ui.*;
import com.codename1.ui.layouts.*;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class SpanLabelLayeredLayoutPreferredSizeTest3000Test extends UITestBase {

    @FormTest
    public void testBorderLayout() {
        Button showPopUp = new Button("Show PopUp in Border Layout");
        Form f = new Form(BoxLayout.y());
        f.setName("testBorderLayout");
        f.add(showPopUp);

        final SpanLabel messageSpanLabel = new SpanLabel("Tap the following button to open the gallery. You should be able to select multiple images and videos. Tap the following button to open the gallery. You should be able to select multiple images and videos.");
        messageSpanLabel.setName("messageSpanLabel");

        showPopUp.addActionListener((e) -> {
            Container centerContainerOuter = new Container(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER));
            centerContainerOuter.add(BorderLayout.CENTER, messageSpanLabel);

            Container layeredPane = f.getLayeredPane();
            layeredPane.setLayout(new LayeredLayout());
            layeredPane.add(centerContainerOuter);
            layeredPane.setVisible(true);

            f.revalidate();
        });
        showPopUp.setName("showBorderLayout");
        f.show();

        // Simulate click
        showPopUp.pressed();
        showPopUp.released();

        Label l = new Label("Tap the following");

        // Wait for layout?? In unit test, revalidate happens synchronously usually if we are on same thread?
        // UITestBase runs tests on current thread, but simulation of EDT might need flushing.
        // revalidate() triggers layout.

        // Force layout just in case
        f.layoutContainer();
        f.getLayeredPane().layoutContainer();

        assertTrue(messageSpanLabel.getHeight() > l.getPreferredH() * 2, "Span Label height is too small. Should be at least a few lines. Got: " + messageSpanLabel.getHeight());
    }
}
