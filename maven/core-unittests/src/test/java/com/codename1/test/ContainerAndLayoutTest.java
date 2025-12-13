package com.codename1.test;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.Layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerAndLayoutTest extends UITestBase {
    @Test
    void containerAddRemoveMaintainsOrder() {
        Container container = new Container(BoxLayout.y());
        Label first = new Label("First");
        Button second = new Button("Second");
        Label third = new Label("Third");

        container.add(first);
        container.add(second);
        container.add(third);

        assertEquals(3, container.getComponentCount());
        assertSame(first, container.getComponentAt(0));
        assertSame(second, container.getComponentAt(1));
        assertSame(third, container.getComponentAt(2));
        assertTrue(container.contains(second));

        container.removeComponent(second);
        assertEquals(2, container.getComponentCount());
        assertSame(third, container.getComponentAt(1));
        assertFalse(container.contains(second));
    }

    @Test
    void borderLayoutStoresConstraints() {
        Container border = new Container(new BorderLayout());
        Label north = new Label("North");
        Label center = new Label("Center");
        Label east = new Label("East");

        border.add(BorderLayout.NORTH, north);
        border.add(BorderLayout.CENTER, center);
        border.add(BorderLayout.EAST, east);

        Layout layout = border.getLayout();
        assertTrue(layout instanceof BorderLayout);
        assertEquals(BorderLayout.NORTH, layout.getComponentConstraint(north));
        assertEquals(BorderLayout.CENTER, layout.getComponentConstraint(center));
        assertEquals(BorderLayout.EAST, layout.getComponentConstraint(east));
    }

    @Test
    void flowLayoutAlignmentAndFillRows() {
        FlowLayout layout = new FlowLayout(Component.CENTER, Component.BOTTOM);
        layout.setAlign(Component.RIGHT);
        layout.setValign(Component.CENTER);
        layout.setFillRows(true);

        assertEquals(Component.RIGHT, layout.getAlign());
        assertEquals(Component.CENTER, layout.getValign());
        assertTrue(layout.isFillRows());
    }

    @Test
    void formScrollableFlagsAndTitle() {
        Form form = new Form("Initial", new BorderLayout());
        form.setTitle("Updated Title");
        form.setScrollableY(false);
        form.setLayout(BoxLayout.y());

        assertEquals("Updated Title", form.getTitle());
        assertFalse(form.isScrollableY());
        assertTrue(form.getLayout() instanceof BoxLayout);
    }

    @Test
    void dialogContentPaneConfiguration() {
        Dialog dialog = new Dialog("Prefs", new BorderLayout());
        dialog.setAutoDispose(false);
        dialog.setDisposeWhenPointerOutOfBounds(true);
        dialog.setDialogUIID("SettingsDialog");

        assertFalse(dialog.isAutoDispose());
        assertTrue(dialog.isDisposeWhenPointerOutOfBounds());
        assertEquals("SettingsDialog", dialog.getDialogUIID());
        assertTrue(dialog.getContentPane().getLayout() instanceof BorderLayout);
    }
}
