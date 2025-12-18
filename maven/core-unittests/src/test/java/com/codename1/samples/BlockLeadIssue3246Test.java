package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.*;

class BlockLeadIssue3246Test extends UITestBase {

    @FormTest
    void blockedComponentReceivesEventsInsteadOfLeadComponent() {
        implementation.setDisplaySize(1080, 1920);

        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        Button main = new Button("Main");
        Button blocked = new Button("Blocked");

        final int[] mainInvocations = {0};
        final int[] blockedInvocations = {0};

        main.addActionListener(e -> mainInvocations[0]++);
        blocked.addActionListener(e -> blockedInvocations[0]++);

        Container container = BorderLayout.centerEastWest(main, blocked, null);
        container.setLeadComponent(main);
        blocked.setBlockLead(true);

        form.add(BorderLayout.CENTER, container);
        form.show();
        form.revalidate();
        flushSerialCalls();
        DisplayTest.flushEdt();
        flushSerialCalls();
        ensureSized(blocked, form);

        tapComponent(blocked);
        flushSerialCalls();
        DisplayTest.flushEdt();
        flushSerialCalls();

        assertEquals(0, mainInvocations[0], "Lead component should not fire when blockLead is true on child");
        assertEquals(1, blockedInvocations[0], "Blocked component should handle its own action when blockLead is true");

        blocked.setBlockLead(false);
        container.setLeadComponent(null);
        container.setLeadComponent(main);
        blockedInvocations[0] = 0;
        mainInvocations[0] = 0;

        form.revalidate();
        flushSerialCalls();
        DisplayTest.flushEdt();

        tapComponent(blocked);
        DisplayTest.flushEdt();
        flushSerialCalls();

        assertEquals(1, mainInvocations[0], "Lead component should receive events from its hierarchy when blockLead is false");
        assertEquals(0, blockedInvocations[0], "Child action listener should not run when lead component handles the event");
    }

    private void ensureSized(Component component, Form form) {
        for (int i = 0; i < 5 && (component.getWidth() <= 0 || component.getHeight() <= 0); i++) {
            form.revalidate();
            flushSerialCalls();
            DisplayTest.flushEdt();
        }
    }
}
