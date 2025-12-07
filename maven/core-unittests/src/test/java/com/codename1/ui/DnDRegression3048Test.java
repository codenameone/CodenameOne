package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for drag and drop interactions when a container sets a lead component
 * that is also draggable. This mimics the behaviour from the DnDRegression3048 sample and
 * ensures drag events propagate through the full stack using {@link Form#pointerPressed(int, int)}
 * and related APIs.
 */
class DnDRegression3048Test extends UITestBase {

    @FormTest
    void dragFinishedFiresWhenDraggingLeadComponent() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();

        Container container = new Container(BoxLayout.y());
        Label imageLabel = new Label("icon");
        imageLabel.setPreferredSize(new Dimension(48, 48));
        imageLabel.setDraggable(true);

        container.putClientProperty("isTest", Boolean.TRUE);
        container.setLeadComponent(imageLabel);
        container.setDraggable(true);

        container.add(imageLabel);
        container.add(new Label("Another label in the container"));
        form.add(container);
        form.revalidate();

        final int[] pointerDraggedCount = {0};
        final int[] dragFinishedCount = {0};
        final int[] leadDragFinishedCount = {0};

        imageLabel.addPointerDraggedListener(evt -> pointerDraggedCount[0]++);
        container.addDragFinishedListener(evt -> dragFinishedCount[0]++);
        imageLabel.addDragFinishedListener(evt -> leadDragFinishedCount[0]++);

        int startX = imageLabel.getAbsoluteX() + 10;
        int startY = imageLabel.getAbsoluteY() + 10;
        int endX = startX + 30;
        int endY = startY + 30;

        form.pointerPressed(startX, startY);
        form.pointerDragged(endX, endY);
        form.pointerDragged(endX + 10, endY + 10);
        form.pointerReleased(endX, endY);

        assertTrue(pointerDraggedCount[0] > 0, "Lead component should receive drag events");
        assertTrue(dragFinishedCount[0] > 0 || leadDragFinishedCount[0] > 0,
                "Drag finished events should fire on the container or its lead component");
    }
}

