package com.codename1.samples;

import com.codename1.components.MultiButton;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.*;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import static com.codename1.ui.ComponentSelector.$;
import static org.junit.jupiter.api.Assertions.*;

public class LeadComponentSampleTest extends UITestBase {
    boolean actionFired = false;
    boolean pointerPressed = false;
    boolean pointerReleased = false;
    boolean longPressed = false;
    Component eventComponent = null;

    @FormTest
    public void testLeadComponent() {
        Form hi = new Form("Hi World", BoxLayout.y());
        Button leadBtn = new Button(">");
        Container cnt = new Container();
        Label label = new Label("Label");
        cnt.setLayout(new BorderLayout());
        cnt.add(BorderLayout.CENTER, label).add(BorderLayout.EAST, leadBtn);

        cnt.setLeadComponent(leadBtn);
        hi.add(cnt);

        Button justButton = new Button("Just Button");
        hi.add(justButton);

        MultiButton mb = new MultiButton("A MultiButton");
        hi.add(mb);

        $(cnt, leadBtn, label, justButton, mb)
                .addActionListener(e->{
                    actionFired = true;
                    eventComponent = $(e).asComponent();
                })
                .addPointerPressedListener(e->{
                    pointerPressed = true;
                    eventComponent = $(e).asComponent();
                })
                .addLongPressListener(e->{
                    longPressed = true;
                    eventComponent = $(e).asComponent();
                    Component c = $(e).asComponent();
                    if (c instanceof Button) {
                        Button b = (Button)c;
                        b.released();
                    }
                })
                .addPointerReleasedListener(e->{
                    pointerReleased = true;
                    eventComponent = $(e).asComponent();
                });

        hi.show();

        // Test Lead Component (Clicking on label should trigger leadBtn action)
        actionFired = false;
        pointerPressed = false;
        pointerReleased = false;
        eventComponent = null;

        // Click on Label
        int x = label.getAbsoluteX() + label.getWidth() / 2;
        int y = label.getAbsoluteY() + label.getHeight() / 2;

        hi.pointerPressed(x, y);
        assertTrue(pointerPressed);
        assertEquals(leadBtn, eventComponent); // Should be reported as leadBtn

        hi.pointerReleased(x, y);
        assertTrue(pointerReleased);
        assertTrue(actionFired);
        assertEquals(leadBtn, eventComponent);

        // Test Just Button
        actionFired = false;
        pointerPressed = false;
        pointerReleased = false;
        eventComponent = null;

        x = justButton.getAbsoluteX() + justButton.getWidth() / 2;
        y = justButton.getAbsoluteY() + justButton.getHeight() / 2;

        hi.pointerPressed(x, y);
        assertTrue(pointerPressed);
        assertEquals(justButton, eventComponent);

        hi.pointerReleased(x, y);
        assertTrue(pointerReleased);
        assertTrue(actionFired);
        assertEquals(justButton, eventComponent);
    }
}
