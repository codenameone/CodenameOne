package com.codename1.ui.list;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ContainerListCoverageTest extends UITestBase {

    @FormTest
    void containerListCoversPropertiesRenderingAndEvents() {
        DefaultListModel<String> model = new DefaultListModel<String>("Alpha", "Beta", "Gamma");
        ContainerList list = new ContainerList(model);
        list.setName("coverageContainerList");
        list.setPreferredW(120);
        list.setPreferredH(80);
        AtomicInteger fired = new AtomicInteger();
        list.addActionListener(new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fired.incrementAndGet();
            }
        });

        Form form = new Form(new BorderLayout());
        form.add(BorderLayout.CENTER, list);
        form.show();

        assertEquals(3, list.getComponentCount());
        assertEquals("ContainerList", list.getUIID());
        assertArrayEquals(new String[]{"ListItems", "Renderer"}, list.getPropertyNames());
        assertEquals(CellRenderer.class, list.getPropertyTypes()[1]);

        CellRenderer renderer = new DefaultListCellRenderer();
        assertNull(list.setPropertyValue("Renderer", renderer));
        assertSame(renderer, list.getRenderer());

        assertNull(list.setPropertyValue("ListItems", new Object[]{"First", "Second"}));
        assertEquals(2, list.getModel().getSize());
        list.setSelectedIndex(1);
        assertEquals("Second", list.getSelectedItem());
        Object[] values = (Object[]) list.getPropertyValue("ListItems");
        assertEquals(2, values.length);

        Component entryCmp = list.getComponentAt(1);
        assertTrue(entryCmp instanceof ContainerList.Entry);
        ContainerList.Entry entry = (ContainerList.Entry) entryCmp;
        entry.pointerReleased(0, 0);
        entry.longPointerPress(0, 0);
        entry.keyReleased(Display.getInstance().getKeyCode(Display.GAME_FIRE));
        assertTrue(fired.get() >= 3);

        list.setScrollable(true);
        assertEquals(Component.DRAG_REGION_POSSIBLE_DRAG_Y, list.getDragRegionStatus(0, 0));
        list.setScrollableX(true);
        assertEquals(Component.DRAG_REGION_POSSIBLE_DRAG_XY, list.getDragRegionStatus(0, 0));
        list.setScrollable(false);
        assertEquals(Component.DRAG_REGION_NOT_DRAGGABLE, list.getDragRegionStatus(0, 0));

        assertNotNull(list.getSelectedRect());
    }
}
