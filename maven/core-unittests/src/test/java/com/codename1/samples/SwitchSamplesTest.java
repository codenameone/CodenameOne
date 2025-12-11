package com.codename1.samples;

import com.codename1.components.Switch;
import com.codename1.components.SwitchList;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestUtils;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.list.DefaultListModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SwitchSamplesTest extends UITestBase {

    @FormTest
    void switchTogglesWhenTapped() {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Label label = new Label("Hi World");
        Switch sw = new Switch();
        final int[] actionCount = {0};
        sw.addActionListener(e -> actionCount[0]++);

        form.add(label);
        form.add(sw);
        ensureLaidOut(form);

        assertFalse(sw.isOn());
        assertEquals(0, actionCount[0]);

        CountDownLatch count = new CountDownLatch(2);
        sw.addActionListener(e -> count.countDown());
        implementation.tapComponent(sw);
        waitFor(count, 1, 2000);
        ensureLaidOut(form);

        assertTrue(sw.isOn(), "Switch should toggle on after tap");
        assertEquals(1, actionCount[0], "Tapping switch should fire action listener");

        implementation.tapComponent(sw);
        waitFor(count, 2000);
        ensureLaidOut(form);

        assertFalse(sw.isOn(), "Second tap should toggle switch off");
        assertEquals(2, actionCount[0], "Action listener should run for each toggle");
    }

    @FormTest
    void switchListFlowLayoutTogglesModelSelection() {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        DefaultListModel<String> model = new DefaultListModel<String>(
                "Red", "Green", "Blue", "Indigo", "Violet", "Orange", "Yellow");
        SwitchList switchList = new SwitchList(model);
        switchList.setLayout(new FlowLayout());
        switchList.refresh();

        form.add(switchList);
        ensureLaidOut(form);

        Component firstCell = switchList.getComponentAt(0);
        Switch firstSwitch = findSwitch(firstCell);
        assertNotNull(firstSwitch, "Decorated cell should contain a Switch");
        assertTrue(switchList.getLayout() instanceof FlowLayout);
        assertFalse(firstSwitch.isOn());

        CountDownLatch latch = new CountDownLatch(2);
        firstSwitch.addActionListener(e -> latch.countDown());
        implementation.tapComponent(firstSwitch);
        waitFor(latch, 1, 2000);
        ensureLaidOut(form);

        assertTrue(firstSwitch.isOn(), "Switch should toggle on through user interaction");
        assertTrue(isIndexSelected(model, 0), "Model selection should track switch state");

        Component thirdCell = switchList.getComponentAt(2);
        Switch thirdSwitch = findSwitch(thirdCell);
        assertNotNull(thirdSwitch);
        thirdSwitch.addActionListener(e -> latch.countDown());
        implementation.tapComponent(thirdSwitch);
        waitFor(latch, 2000);
        ensureLaidOut(form);

        assertTrue(thirdSwitch.isOn());
        assertTrue(isIndexSelected(model, 2), "Selecting another entry should keep multiple selections");
    }

    @FormTest
    void switchesRemainInteractiveWhileScrolling() throws InterruptedException {
        implementation.setDisplaySize(480, 800);
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        form.setScrollableY(true);

        List<Switch> switches = new ArrayList<Switch>();
        for (int i = 1; i <= 60; i++) {
            Container row = new Container(new GridLayout(2));
            row.add(new Label("Line " + i));
            Switch sw = new Switch();
            switches.add(sw);
            row.add(sw);
            form.add(row);
        }
        ensureLaidOut(form);

        Switch target = switches.get(40);
        implementation.dispatchScrollToVisible(form.getContentPane(), target.getY());
        ensureLaidOut(form);


        boolean initialState = target.isOn();
        ensureLaidOut(form);
        final CountDownLatch latch = new CountDownLatch(1);
        target.addActionListener(ev -> latch.countDown());
        implementation.tapComponent(target);
        waitFor(latch, 2000);
        assertNotEquals(initialState, target.isOn(), "Switch should toggle even after scrolling");
        assertFalse(switches.get(2).isOn(), "Unrelated switches should not toggle during scrolling");
    }

    private void ensureLaidOut(Form form) {
        form.revalidate();
        flushSerialCalls();
        DisplayTest.flushEdt();
        flushSerialCalls();
    }

    private Switch findSwitch(Component component) {
        if (component instanceof Switch) {
            return (Switch) component;
        }
        if (component instanceof Container) {
            Container container = (Container) component;
            int count = container.getComponentCount();
            for (int i = 0; i < count; i++) {
                Switch found = findSwitch(container.getComponentAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private boolean isIndexSelected(DefaultListModel<String> model, int index) {
        int[] selected = model.getSelectedIndices();
        for (int value : selected) {
            if (value == index) {
                return true;
            }
        }
        return false;
    }
}
