package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionEvent;

import static org.junit.jupiter.api.Assertions.*;

class NavigationCommandTest extends UITestBase {

    private static class TrackingForm extends Form {
        private int showCount;

        TrackingForm(String title) {
            super(title);
        }

        @Override
        public void show() {
            showCount++;
        }
    }

    @FormTest
    void testActionNavigatesToNextForm() {
        NavigationCommand command = new NavigationCommand("Next");
        TrackingForm next = new TrackingForm("NextForm");
        command.setNextForm(next);
        command.actionPerformed(new ActionEvent(command));
        assertEquals(1, next.showCount);
    }

    @FormTest
    void testConstructors() {
        Image icon = Image.createImage(10, 10);
        NavigationCommand commandWithIcon = new NavigationCommand("Icon", icon);
        assertEquals("Icon", commandWithIcon.getCommandName());
        assertSame(icon, commandWithIcon.getIcon());

        NavigationCommand commandWithId = new NavigationCommand("Id", 5);
        assertEquals(5, commandWithId.getId());

        NavigationCommand full = new NavigationCommand("Full", icon, 7);
        assertEquals(7, full.getId());
        assertSame(icon, full.getIcon());
    }
}
