package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.ComponentSelector;
import com.codename1.ui.ComponentSelector.ComponentClosure;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import java.util.Iterator;

import static com.codename1.ui.ComponentSelector.$;
import static org.junit.jupiter.api.Assertions.*;

class TooltipSelectionComponentTest extends UITestBase {

    @FormTest
    void tooltipManagerSchedulesAndClearsTooltips() {
        implementation.setBuiltinSoundsEnabled(false);

        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        Label label = new Label("Hover");
        label.setPreferredSize(new Dimension(80, 20));
        form.add(BorderLayout.CENTER, label);
        form.revalidate();

        TrackingTooltipManager manager = new TrackingTooltipManager();
        TooltipManager.enableTooltips(manager);

        assertSame(manager, TooltipManager.getInstance(), "Custom tooltip manager should be registered");

        manager.setTooltipShowDelay(1250);
        manager.setDialogUIID("DialogUIID");
        manager.setTextUIID("TextUIID");

        manager.showImmediately("Helpful tip", label);
        assertEquals(1, manager.showCount, "showTooltip should be invoked once");
        assertEquals("Helpful tip", manager.lastTip);
        assertSame(label, manager.lastComponent);

        manager.clearTooltip();
        assertEquals(1, manager.clearCount, "clearTooltip should track invocations");
        assertEquals(1250, manager.getTooltipShowDelay());
        assertEquals("DialogUIID", manager.getDialogUIID());
        assertEquals("TextUIID", manager.getTextUIID());
    }

    @FormTest
    void textSelectionEnablesAndCopiesSelectedText() {
        implementation.setBuiltinSoundsEnabled(false);
        implementation.resetTextSelectionTracking();

        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        TextArea area = new TextArea("Hello Selection World");
        area.setEditable(false);
        area.setTextSelectionEnabled(true);
        form.add(BorderLayout.CENTER, area);
        form.revalidate();

        TextSelection selection = form.getTextSelection();
        final ActionEvent.Type[] eventType = {null};
        selection.addTextSelectionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                eventType[0] = evt.getEventType();
            }
        });

        selection.setEnabled(true);
        assertEquals(1, implementation.getInitializeTextSelectionCount(), "initializeTextSelection should be called once");
        assertSame(selection, implementation.getLastInitializedTextSelection());

        selection.selectAll();
        assertEquals(ActionEvent.Type.Change, eventType[0], "Selection listener should receive change events");
        assertEquals("Hello Selection World", selection.getSelectionAsText());

        selection.copy();
        assertEquals(1, implementation.getCopySelectionInvocations());
        assertSame(selection, implementation.getLastCopiedTextSelection());
        assertEquals("Hello Selection World", implementation.getLastCopiedText());

        selection.setIgnoreEvents(true);
        selection.setIgnoreEvents(false);

        selection.setEnabled(false);
        assertEquals(1, implementation.getDeinitializeTextSelectionCount());
        assertSame(selection, implementation.getLastDeinitializedTextSelection());
    }

    @FormTest
    void componentSelectorQueriesAndMutationsAffectComponents() {
        implementation.setBuiltinSoundsEnabled(false);

        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        Container container = new Container(BoxLayout.y());
        Label label = new Label("Primary");
        label.setUIID("InfoLabel");
        label.setName("labelOne");
        Button button = new Button("Action");
        button.setName("targetButton");
        container.add(label);
        container.add(button);
        form.add(BorderLayout.CENTER, container);
        form.revalidate();

        $(label).addTags("important");
        ComponentSelector important = $(".important", form);
        assertTrue(important.contains(label));

        ComponentSelector labels = $("Label", form);
        labels.each(new ComponentClosure() {
            public void call(Component c) {
                c.getAllStyles().setFgColor(0xff00ff);
            }
        });
        assertEquals(0xff00ff, label.getUnselectedStyle().getFgColor());

        ComponentSelector parents = $(label).getParent();
        assertTrue(parents.contains(container));

        $("#targetButton", form).setEnabled(false);
        assertFalse(button.isEnabled());

        final int[] invocationCount = {0};
        $("Label, Button", form).each(new ComponentClosure() {
            public void call(Component c) {
                invocationCount[0]++;
                c.setVisible(true);
            }
        });
        assertEquals(2, invocationCount[0]);

        Iterator<Component> iterator = $("Container > *", container).iterator();
        assertTrue(iterator.hasNext());
        assertSame(label, iterator.next());
        assertTrue(iterator.hasNext());
        assertSame(button, iterator.next());
        assertFalse(iterator.hasNext());
    }

    private static class TrackingTooltipManager extends TooltipManager {
        int showCount;
        int clearCount;
        String lastTip;
        Component lastComponent;

        void showImmediately(String tip, Component cmp) {
            showTooltip(tip, cmp);
        }

        @Override
        protected void showTooltip(String tip, Component cmp) {
            showCount++;
            lastTip = tip;
            lastComponent = cmp;
            super.showTooltip(tip, cmp);
        }

        @Override
        protected void clearTooltip() {
            clearCount++;
            super.clearTooltip();
        }
    }
}
