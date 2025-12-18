package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

class AddComponentToRightBarSampleTest extends UITestBase {

    @FormTest
    void rightBarCommandCanBeTriggeredThroughToolbarComponent() {
        implementation.setDisplaySize(1080, 1920);

        boolean originalGlobalToolbar = Toolbar.isGlobalToolbar();
        Toolbar.setGlobalToolbar(true);
        Form form = new Form("Hi World", BoxLayout.y());
        Toolbar toolbar = form.getToolbar();
        try {
            final int[] invocations = {0};
            Command command = toolbar.addCommandToRightBar("Test", null, evt -> invocations[0]++);

            form.add(new Label("Hi World"));
            form.show();
            form.revalidate();
            flushSerialCalls();
            DisplayTest.flushEdt();
            flushSerialCalls();

            Component commandComponent = toolbar.findCommandComponent(command);
            assertNotNull(commandComponent);
            ensureSized(commandComponent, form);

            tapComponent(commandComponent);
            flushSerialCalls();
            DisplayTest.flushEdt();
            flushSerialCalls();

            assertEquals(1, invocations[0]);
        } finally {
            Toolbar.setGlobalToolbar(originalGlobalToolbar);
        }
    }

    @FormTest
    void commandComponentCanBeReplacedWithCustomButton() {
        implementation.setDisplaySize(1080, 1920);

        boolean originalGlobalToolbar = Toolbar.isGlobalToolbar();
        Toolbar.setGlobalToolbar(true);
        Form form = new Form("Hi World", BoxLayout.y());
        Toolbar toolbar = form.getToolbar();
        try {
            Command command = toolbar.addCommandToRightBar("Test", null, evt -> { });

            form.add(new Label("Hi World"));
            form.show();
            form.revalidate();
            flushSerialCalls();
            DisplayTest.flushEdt();
            flushSerialCalls();

            Component commandComponent = toolbar.findCommandComponent(command);
            assertNotNull(commandComponent);
            ensureSized(commandComponent, form);

            Button replacement = new Button("Replaced cmp");
            final boolean[] replacementInvoked = {false};
            replacement.addActionListener(evt -> replacementInvoked[0] = true);

            Container rightBarContainer = commandComponent.getParent();
            assertNotNull(rightBarContainer);
            rightBarContainer.replace(commandComponent, replacement, null);
            form.revalidate();
            flushSerialCalls();

            assertSame(rightBarContainer, replacement.getParent());
            assertFalse(rightBarContainer.contains(commandComponent));

            ensureSized(replacement, form);
            tapComponent(replacement);
            flushSerialCalls();
            DisplayTest.flushEdt();
            flushSerialCalls();

            assertTrue(replacementInvoked[0]);
        } finally {
            Toolbar.setGlobalToolbar(originalGlobalToolbar);
        }
    }

    private void ensureSized(Component component, Form form) {
        for (int i = 0; i < 5 && (component.getWidth() <= 0 || component.getHeight() <= 0); i++) {
            form.revalidate();
            flushSerialCalls();
        }
    }
}

