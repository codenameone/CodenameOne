package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import static org.junit.jupiter.api.Assertions.*;

class DragFinishedListener3056Test extends UITestBase {

    @FormTest
    void dragFinishedListenerFiresForLeadComponent() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        form.removeAll();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Container container = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        container.setPreferredSize(new Dimension(160, 160));

        Label draggableIcon = new Label();
        FontImage.setMaterialIcon(draggableIcon, FontImage.MATERIAL_3D_ROTATION, 10);
        draggableIcon.setPreferredSize(new Dimension(100, 100));
        draggableIcon.setDraggable(true);

        Label secondary = new Label("Another label in the container");

        container.add(draggableIcon);
        container.add(secondary);
        container.putClientProperty("isTest", Boolean.TRUE);
        container.setLeadComponent(draggableIcon);
        container.setDraggable(true);

        final int[] dragCount = {0};
        final boolean[] dragFinishedCalled = {false};
        final int[] finished = {-1, -1};
        final Component[] eventSource = {null};

        draggableIcon.addPointerDraggedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                dragCount[0]++;
            }
        });

        draggableIcon.addDragFinishedListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                dragFinishedCalled[0] = true;
                finished[0] = evt.getX();
                finished[1] = evt.getY();
                eventSource[0] = (Component) evt.getSource();
            }
        });

        form.add(container);
        form.revalidate();

        int startX = draggableIcon.getAbsoluteX() + draggableIcon.getWidth() / 2;
        int startY = draggableIcon.getAbsoluteY() + draggableIcon.getHeight() / 2;
        int dragX = startX + 15;
        int dragY = startY + 15;
        int releaseX = startX + 25;
        int releaseY = startY + 20;

        implementation.dispatchPointerPress(startX, startY);
        implementation.setHasDragStarted(true);
        flushSerialCalls();
        implementation.dispatchPointerDrag(dragX, dragY);
        implementation.dispatchPointerDrag(releaseX, releaseY);
        implementation.dispatchPointerRelease(releaseX, releaseY);
        flushSerialCalls();

        assertEquals(Boolean.TRUE, container.getClientProperty("isTest"),
                "Container should preserve client property used by the sample");
        assertTrue(dragCount[0] > 0, "Pointer dragged listener should be invoked while dragging");
        assertTrue(dragFinishedCalled[0], "Drag finished listener should fire after releasing drag");
        assertSame(draggableIcon, eventSource[0], "Event source should match the draggable lead component");
        assertTrue(finished[0] >= 0 && finished[1] >= 0,
                "Drag finished coordinates should be provided with non-negative values");
    }
}
